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
        JsonNode data;
        try {
            data = tornApiClient.fetchHouseEvents(cutoffEpoch);
        } catch (Exception e) {
            log.warn("House events API failed: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "Could not read house account events. Check TORN_API_MY_KEY has Events access.");
            return result;
        }

        JsonNode events = resolveEventsNode(data);
        if (events == null || events.isMissingNode() || events.isNull() || !events.fields().hasNext()) {
            result.put("success", true);
            result.put("message",
                    "No events returned from Torn. Confirm the house API key includes the Events selection, "
                            + "then send Xanax with message \"" + depositProperties.getRequiredMessage() + "\".");
            result.put("site_balance", user.getSiteBalance());
            result.put("new_moola", 0);
            return result;
        }

        String requiredMessage = depositProperties.getRequiredMessage();
        int moolaPerXanax = gameProperties.getMoolaPerXanax();
        long totalMoola = 0;
        int processedCount = 0;

        Iterator<Map.Entry<String, JsonNode>> fields = events.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String eventId = entry.getKey();
            JsonNode event = entry.getValue();

            if (eventId == null || eventId.isBlank()) {
                if (event.has("id")) {
                    eventId = String.valueOf(event.get("id").asInt());
                } else {
                    continue;
                }
            }

            long eventTimestamp = event.path("timestamp").asLong(0);
            if (eventTimestamp > 0 && eventTimestamp < cutoffEpoch) {
                continue;
            }

            String eventText = event.path("event").asText("");
            Optional<ParsedDeposit> parsed = TornXanaxDepositParser.parse(eventText, requiredMessage);
            if (parsed.isEmpty()) {
                continue;
            }

            ParsedDeposit deposit = parsed.get();
            if (!TornXanaxDepositParser.tornIdsMatch(deposit.senderTornId(), userTornId)) {
                continue;
            }
            if (depositRepository.existsByEventId(eventId)) {
                continue;
            }

            long moola = (long) deposit.xanaxAmount() * moolaPerXanax;
            Instant tornEventTime = eventTimestamp > 0
                    ? Instant.ofEpochSecond(eventTimestamp)
                    : Instant.now();
            Deposit record = new Deposit(eventId, userTornId, "player", deposit.xanaxAmount(), moola, tornEventTime);
            record.setUser(user);
            depositRepository.save(record);

            totalMoola += moola;
            processedCount++;
            log.info("Credited deposit event {} — {} xanax from tornId {}", eventId, deposit.xanaxAmount(), userTornId);
        }

        if (processedCount == 0) {
            result.put("success", true);
            result.put("message",
                    "No new deposits found. Send Xanax to the house account with message \""
                            + requiredMessage
                            + "\" (exact spelling; rps and RPS both work). Wait a minute after sending, then try again.");
            result.put("site_balance", user.getSiteBalance());
            result.put("new_moola", 0);
            return result;
        }

        user.setSiteBalance(user.getSiteBalance() + totalMoola);
        userRepository.save(user);
        result.put("success", true);
        result.put("message", "Verified " + processedCount + " deposit(s): "
                + (totalMoola / moolaPerXanax) + " Xanax (" + totalMoola + " Moola)");
        result.put("site_balance", user.getSiteBalance());
        result.put("new_moola", totalMoola);
        return result;
    }

    /** Torn returns {@code events} at root; some clients nest under {@code data}. */
    private JsonNode resolveEventsNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode events = root.path("events");
        if (!events.isMissingNode() && events.isObject()) {
            return events;
        }
        return root.path("data").path("events");
    }
}
