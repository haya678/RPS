package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.config.TornDepositProperties;
import com.xanwar.rps.model.Deposit;
import com.xanwar.rps.model.PendingDeposit;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.DepositRepository;
import com.xanwar.rps.repository.PendingDepositRepository;
import com.xanwar.rps.repository.UserRepository;
import com.xanwar.rps.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositService.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final DepositRepository depositRepository;
    private final PendingDepositRepository pendingDepositRepository;
    private final TornApiClient tornApiClient;
    private final TornDepositProperties depositProperties;
    private final GameProperties gameProperties;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    // Local cache for pending monitors to avoid redundant tasks
    private final Map<String, ScheduledFuture<?>> activeMonitors = new ConcurrentHashMap<>();

    public DepositService(
            UserService userService,
            UserRepository userRepository,
            DepositRepository depositRepository,
            PendingDepositRepository pendingDepositRepository,
            TornApiClient tornApiClient,
            TornDepositProperties depositProperties,
            GameProperties gameProperties,
            TaskScheduler taskScheduler,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate
    ) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
        this.pendingDepositRepository = pendingDepositRepository;
        this.tornApiClient = tornApiClient;
        this.depositProperties = depositProperties;
        this.gameProperties = gameProperties;
        this.taskScheduler = taskScheduler;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> initiateDeposit(String userTornId, int xanaxAmount) {
        User user = userService.requireUser(userTornId);

        // Check if already pending
        if (pendingDepositRepository.existsByTornId(userTornId)) {
            return ApiResponse.error("A deposit is already pending for your account.");
        }

        PendingDeposit pending = new PendingDeposit(userTornId, xanaxAmount);
        pendingDepositRepository.save(pending);

        log.info("[DEPOSIT] User {} initiated a deposit for {} Xanax.", userTornId, xanaxAmount);

        // Notify cluster
        broadcastSync("add", userTornId, Map.of("itemType", "Xanax", "xanaxAmount", xanaxAmount));

        // Start background monitoring
        startMonitoring(userTornId, xanaxAmount, pending.getStartTime());

        return ApiResponse.success("message", "Monitoring for Xanax...");
    }

    public Map<String, Object> checkStatus(String userTornId) {
        boolean pending = pendingDepositRepository.existsByTornId(userTornId);
        return Map.of("status", pending ? "pending" : "not_pending");
    }

    @Transactional
    public Map<String, Object> cancelDeposit(String userTornId) {
        pendingDepositRepository.deleteByTornId(userTornId);
        ScheduledFuture<?> future = activeMonitors.remove(userTornId);
        if (future != null) {
            future.cancel(false);
        }
        broadcastSync("remove", userTornId, null);
        return ApiResponse.success();
    }

    private void startMonitoring(String tornId, int expectedAmount, Instant startTime) {
        long monitorUntil = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes

        ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(() -> {
            try {
                if (System.currentTimeMillis() > monitorUntil) {
                    stopMonitoring(tornId, "timeout");
                    return;
                }

                if (!pendingDepositRepository.existsByTornId(tornId)) {
                    stopMonitoring(tornId, "cancelled");
                    return;
                }

                pollAndVerify(tornId, expectedAmount, startTime);
            } catch (Exception e) {
                log.error("[DEPOSIT] Error in monitor for {}: {}", tornId, e.getMessage());
            }
        }, Instant.now().plusSeconds(5), java.time.Duration.ofSeconds(20));

        activeMonitors.put(tornId, future);
    }

    private void stopMonitoring(String tornId, String reason) {
        log.info("[DEPOSIT] Monitoring stopped for {} due to {}", tornId, reason);
        ScheduledFuture<?> future = activeMonitors.remove(tornId);
        if (future != null) {
            future.cancel(false);
        }
        if ("timeout".equals(reason)) {
            pendingDepositRepository.deleteByTornId(tornId);
            broadcastSync("remove", tornId, null);
        }
    }

    public void pollAndVerify() {
        log.info("[DEPOSIT] Manual poll triggered.");
        JsonNode data;
        try {
            data = tornApiClient.fetchHouseActivity();
        } catch (Exception e) {
            log.error("[DEPOSIT] Torn API fetch failed: {}", e.getMessage());
            return;
        }

        JsonNode events = data.path("events");
        if (!events.isObject()) return;

        parseAndProcessEvents(events);
    }

    private void parseAndProcessEvents(JsonNode events) {
        Iterator<Map.Entry<String, JsonNode>> fields = events.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String eventId = entry.getKey();
            JsonNode event = entry.getValue();
            String eventText = event.path("event").asText("");
            long eventTimestamp = event.path("timestamp").asLong() * 1000;

            if (!eventText.contains("Xanax")) continue;

            Matcher senderMatch = Pattern.compile("XID=(\\d+)").matcher(eventText);
            if (!senderMatch.find()) continue;
            String senderId = senderMatch.group(1);

            Matcher amountMatch = Pattern.compile("You were sent (?:([\\d,]+)x|some) Xanax from", Pattern.CASE_INSENSITIVE).matcher(eventText);
            
            if (amountMatch.find()) {
                int sentAmount = 0;
                String amtStr = amountMatch.group(1);
                if (amtStr != null) {
                    sentAmount = Integer.parseInt(amtStr.replace(",", ""));
                } else if (eventText.contains("some")) {
                    sentAmount = 1;
                }

                if (sentAmount == 0) continue;
                
                // For manual verify, we only process if the user has an account
                if (userRepository.existsByTornId(senderId)) {
                    claimDeposit(senderId, eventId, sentAmount, Instant.ofEpochMilli(eventTimestamp));
                }
            }
        }
    }

    private void pollAndVerify(String tornId, int expectedAmount, Instant startTime) {
        log.debug("[DEPOSIT DEBUG] Polling Torn API for User {}...", tornId);
        JsonNode data;
        try {
            data = tornApiClient.fetchHouseActivity();
        } catch (Exception e) {
            log.error("[DEPOSIT] Torn API fetch failed: {}", e.getMessage());
            return;
        }

        JsonNode events = data.path("events");
        if (!events.isObject()) return;

        Iterator<Map.Entry<String, JsonNode>> fields = events.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String eventId = entry.getKey();
            JsonNode event = entry.getValue();
            String eventText = event.path("event").asText("");
            long eventTimestamp = event.path("timestamp").asLong() * 1000;

            // Simple regex match as requested in the snippet
            if (!eventText.contains("Xanax")) continue;

            // Extract sender ID from XID=...
            Matcher senderMatch = Pattern.compile("XID=(\\d+)").matcher(eventText);
            if (!senderMatch.find()) continue;
            String senderId = senderMatch.group(1);

            // Extract amount
            // Matcher amountMatch = Pattern.compile("You were sent (?:([\\d,]+)x|some) Xanax from").matcher(eventText);
            // Wait, I should use the robust parser or the requested regex. 
            // The requested regex was: /You were sent (?:([\d,]+)x|some) Xanax from/
            Matcher amountMatch = Pattern.compile("You were sent (?:([\\d,]+)x|some) Xanax from", Pattern.CASE_INSENSITIVE).matcher(eventText);
            
            if (amountMatch.find()) {
                int sentAmount = 0;
                String amtStr = amountMatch.group(1);
                if (amtStr != null) {
                    sentAmount = Integer.parseInt(amtStr.replace(",", ""));
                } else if (eventText.contains("some")) {
                    sentAmount = 1;
                }

                if (sentAmount == 0) continue;
                if (eventTimestamp <= (startTime.toEpochMilli() - 10000)) continue;
                if (!tornId.equals(senderId)) continue;

                if (sentAmount == expectedAmount) {
                    claimDeposit(tornId, eventId, sentAmount, Instant.ofEpochMilli(eventTimestamp));
                    return;
                }
            }
        }
    }

    @Transactional
    protected void claimDeposit(String tornId, String eventId, int amount, Instant tornTime) {
        // Unique check via DB
        String uniqueId = "auto-" + eventId;
        if (depositRepository.existsByEventId(uniqueId)) return;

        // Atomic claim: delete pending deposit. If it fails, another process/thread won it.
        if (!pendingDepositRepository.existsByTornId(tornId)) return;
        pendingDepositRepository.deleteByTornId(tornId);

        User user = userRepository.findByTornId(tornId).orElseThrow();
        long value = amount * depositProperties.getXanaxValue();

        user.setSiteBalance(user.getSiteBalance() + value);
        userRepository.save(user);

        Deposit record = new Deposit(uniqueId, tornId, user.getUsername(), amount, value, tornTime);
        record.setUser(user);
        depositRepository.save(record);

        log.info("[DEPOSIT] Confirmed {} Xanax for user {}. Value: {}", amount, tornId, value);
        
        // Sync and stop monitoring
        stopMonitoring(tornId, "confirmed");
        broadcastSync("confirm", eventId, null);
    }

    private void broadcastSync(String type, String id, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("tornId", id);
            if (data != null) payload.put("data", data);

            String json = objectMapper.writeValueAsString(payload);
            jdbcTemplate.execute("SELECT pg_notify('pending_deposit_sync', ?)", (org.springframework.jdbc.core.PreparedStatementCallback<Void>) ps -> {
                ps.setString(1, json);
                ps.execute();
                return null;
            });
        } catch (Exception e) {
            log.warn("Sync broadcast failed: {}", e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> verifyDeposit(String userTornId) {
        if (userTornId == null || userTornId.isBlank()) {
            return ApiResponse.error("Not authenticated");
        }
        
        // Manual trigger: poll for new events and process them
        pollAndVerify();
        
        User user = userService.requireUser(userTornId);
        return Map.of(
            "success", true,
            "site_balance", user.getSiteBalance(),
            "message", "Verification triggered. Check your balance."
        );
    }
}
