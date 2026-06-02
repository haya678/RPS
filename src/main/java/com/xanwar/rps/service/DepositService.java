package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.config.TornDepositProperties;
import com.xanwar.rps.game.TornXanaxDepositParser;
import com.xanwar.rps.game.TornXanaxDepositParser.ParsedDeposit;
import com.xanwar.rps.model.Deposit;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.DepositRepository;
import com.xanwar.rps.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Service
public class DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositService.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final DepositRepository depositRepository;
    private final TornApiClient tornApiClient;
    private final TornDepositProperties depositProperties;
    private final GameProperties gameProperties;

    public DepositService(
            UserService userService,
            UserRepository userRepository,
            DepositRepository depositRepository,
            TornApiClient tornApiClient,
            TornDepositProperties depositProperties,
            GameProperties gameProperties
    ) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
        this.tornApiClient = tornApiClient;
        this.depositProperties = depositProperties;
        this.gameProperties = gameProperties;
    }

    @Transactional
    public Map<String, Object> verifyDeposit(String userTornId) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.requireUser(userTornId);

        long cutoffEpoch = Instant.now().getEpochSecond()
                - (depositProperties.getMaxAgeHours() * 3600L);
        log.info("Verifying deposit for user tornId={}, cutoff={}h ago (epoch={})",
                userTornId, depositProperties.getMaxAgeHours(), cutoffEpoch);

        JsonNode data;
        try {
            data = tornApiClient.fetchHouseActivity();
        } catch (Exception e) {
            log.warn("House activity API failed: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Could not read house Torn events. Check TORN_API_MY_KEY has full access (info, events, log). Error: " + e.getMessage());
            return result;
        }

        String requiredMessage = depositProperties.getRequiredMessage();
        int moolaPerXanax = gameProperties.getMoolaPerXanax();
        long totalMoola = 0;

        int eventsCount = data.path("events").isObject() ? data.path("events").size() : 0;
        int logCount = data.path("log").isObject() ? data.path("log").size() : 0;
        log.info("Torn API returned {} events, {} log entries", eventsCount, logCount);

        totalMoola += processActivityNode(
                data.path("events"), user, userTornId, cutoffEpoch, requiredMessage, moolaPerXanax, "events");
        totalMoola += processActivityNode(
                data.path("log"), user, userTornId, cutoffEpoch, requiredMessage, moolaPerXanax, "log");

        if (totalMoola == 0) {
            result.put("success", true);
            result.put("verified", false);
            StringBuilder msg = new StringBuilder();
            msg.append("No new deposits found for Torn ID ").append(userTornId).append(". ");
            msg.append("Send Xanax to ").append(depositProperties.getRecipientName());
            msg.append(" [").append(depositProperties.getRecipientId()).append("] with message \"");
            msg.append(requiredMessage);
            msg.append("\" from the same account you logged in with, wait 1\u20132 minutes, then try again.");
            result.put("message", msg.toString());
            result.put("site_balance", user.getSiteBalance());
            result.put("new_moola", 0);
            result.put("moola_per_xanax", moolaPerXanax);
            log.info("No deposits matched for user {}", userTornId);
            return result;
        }

        int xanaxCredited = (int) (totalMoola / moolaPerXanax);
        user.setSiteBalance(user.getSiteBalance() + totalMoola);
        userRepository.save(user);
        result.put("success", true);
        result.put("verified", true);
        result.put("xanax_amount", xanaxCredited);
        result.put("moola_credited", totalMoola);
        result.put("moola_per_xanax", moolaPerXanax);
        result.put("message", "Deposit verified! "
                + xanaxCredited + " Xanax \u2192 " + totalMoola + " Moola");
        result.put("site_balance", user.getSiteBalance());
        result.put("new_moola", totalMoola);
        log.info("Credited {} moola ({} xanax) to user {}", totalMoola, xanaxCredited, userTornId);
        return result;
    }

    private long processActivityNode(
            JsonNode node,
            User user,
            String userTornId,
            long cutoffEpoch,
            String requiredMessage,
            int moolaPerXanax,
            String source
    ) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            log.debug("[{}] No node to process (missing/null/not-object)", source);
            return 0L;
        }

        long totalMoola = 0L;
        int processedCount = 0;
        int skippedOld = 0;
        int skippedNoText = 0;
        int skippedNoMatch = 0;
        int skippedWrongUser = 0;
        int skippedDuplicate = 0;

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String eventId = entry.getKey();
            JsonNode item = entry.getValue();

            if (eventId == null || eventId.isBlank()) {
                if (item.has("id")) {
                    eventId = String.valueOf(item.get("id").asInt());
                } else {
                    continue;
                }
            }

            long eventTimestamp = item.path("timestamp").asLong(0);
            if (eventTimestamp == 0) {
                eventTimestamp = item.path("time").asLong(0);
            }
            if (eventTimestamp > 0 && eventTimestamp < cutoffEpoch) {
                skippedOld++;
                continue;
            }

            // Torn events use "event" field, log entries use "log" field
            String eventText = item.path("event").asText("");
            if (eventText.isBlank()) {
                eventText = item.path("log").asText("");
            }
            if (eventText.isBlank()) {
                eventText = item.path("data").asText("");
            }
            if (eventText.isBlank()) {
                eventText = item.path("title").asText("");
            }
            if (eventText.isBlank()) {
                skippedNoText++;
                continue;
            }

            // Only look at entries mentioning Xanax to avoid unnecessary parsing
            if (!eventText.toLowerCase().contains("xanax")) {
                skippedNoMatch++;
                continue;
            }

            log.debug("[{}] Event {}: {}", source, eventId,
                    eventText.length() > 120 ? eventText.substring(0, 120) + "..." : eventText);

            Optional<ParsedDeposit> parsed = TornXanaxDepositParser.parse(eventText, requiredMessage);
            if (parsed.isEmpty()) {
                skippedNoMatch++;
                continue;
            }

            ParsedDeposit deposit = parsed.get();
            if (!TornXanaxDepositParser.tornIdsMatch(deposit.senderTornId(), userTornId)) {
                log.debug("[{}] Skipping deposit from tornId {} \u2014 logged-in user is {}",
                        source, deposit.senderTornId(), userTornId);
                skippedWrongUser++;
                continue;
            }

            String uniqueId = "ev-" + eventId;
            if (depositRepository.existsByEventId(uniqueId)) {
                skippedDuplicate++;
                continue;
            }

            long moola = (long) deposit.xanaxAmount() * moolaPerXanax;
            Instant tornEventTime = eventTimestamp > 0
                    ? Instant.ofEpochSecond(eventTimestamp)
                    : Instant.now();
            Deposit record = new Deposit(uniqueId, userTornId, user.getUsername(), deposit.xanaxAmount(), moola, tornEventTime);
            record.setUser(user);
            depositRepository.save(record);

            totalMoola += moola;
            log.info("[{}] Matched deposit {} \u2014 {} xanax from tornId {} for user {} = {} moola",
                    source, uniqueId, deposit.xanaxAmount(), deposit.senderTornId(), userTornId, moola);
            processedCount++;
        }

        log.info("[{}] Done: processed={}, matched={}, skipped: old={}, noText={}, noMatch={}, wrongUser={}, duplicate={}",
                source, processedCount, totalMoola / moolaPerXanax,
                skippedOld, skippedNoText, skippedNoMatch, skippedWrongUser, skippedDuplicate);
        return totalMoola;
    }
}
