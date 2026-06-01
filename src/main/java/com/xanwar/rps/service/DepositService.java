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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
            data = tornApiClient.fetchHouseActivity(cutoffEpoch);
        } catch (Exception e) {
            log.warn("House activity API failed: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "Could not read Hannath's Torn events. Check TORN_API_MY_KEY has Events + Log access.");
            return result;
        }

        String requiredMessage = depositProperties.getRequiredMessage();
        int moolaPerXanax = gameProperties.getMoolaPerXanax();
        List<String> debugHints = new ArrayList<>();

        long totalMoola = 0;
        totalMoola += processActivityNode(
                data.path("events"), user, userTornId, cutoffEpoch, requiredMessage, moolaPerXanax, debugHints);
        totalMoola += processActivityNode(
                data.path("log"), user, userTornId, cutoffEpoch, requiredMessage, moolaPerXanax, debugHints);
        totalMoola += processActivityNode(
                data.path("data").path("events"), user, userTornId, cutoffEpoch, requiredMessage, moolaPerXanax, debugHints);
        totalMoola += processActivityNode(
                data.path("data").path("log"), user, userTornId, cutoffEpoch, requiredMessage, moolaPerXanax, debugHints);

        if (totalMoola == 0) {
            result.put("success", true);
            StringBuilder msg = new StringBuilder();
            msg.append("No new deposits found for Torn ID ").append(userTornId).append(". ");
            msg.append("Send Xanax to Hannath [3961385] with message \"").append(requiredMessage);
            msg.append("\" from the same account you logged in with, wait 1–2 minutes, then try again.");
            if (!debugHints.isEmpty()) {
                msg.append(" Recent Xanax activity on house account: ").append(String.join(" | ", debugHints.subList(0, Math.min(2, debugHints.size()))));
            }
            result.put("message", msg.toString());
            result.put("site_balance", user.getSiteBalance());
            result.put("new_moola", 0);
            return result;
        }

        user.setSiteBalance(user.getSiteBalance() + totalMoola);
        userRepository.save(user);
        result.put("success", true);
        result.put("message", "Verified deposit: "
                + (totalMoola / moolaPerXanax) + " Xanax → " + totalMoola + " Moola");
        result.put("site_balance", user.getSiteBalance());
        result.put("new_moola", totalMoola);
        return result;
    }

    private long processActivityNode(
            JsonNode node,
            User user,
            String userTornId,
            long cutoffEpoch,
            String requiredMessage,
            int moolaPerXanax,
            List<String> debugHints
    ) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return 0L;
        }

        long totalMoola = 0L;

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
                continue;
            }

            String eventText = item.path("event").asText("");
            if (eventText.isBlank()) {
                eventText = item.path("data").asText("");
            }
            if (eventText.isBlank()) {
                eventText = item.path("title").asText("");
            }

            debugHints.addAll(TornXanaxDepositParser.findXanaxHints(eventText));

            Optional<ParsedDeposit> parsed = TornXanaxDepositParser.parse(eventText, requiredMessage);
            if (parsed.isEmpty()) {
                continue;
            }

            ParsedDeposit deposit = parsed.get();
            if (!TornXanaxDepositParser.tornIdsMatch(deposit.senderTornId(), userTornId)) {
                log.debug("Skipping deposit from tornId {} — logged-in user is {}", deposit.senderTornId(), userTornId);
                continue;
            }

            String uniqueId = "ev-" + eventId;
            if (depositRepository.existsByEventId(uniqueId)) {
                continue;
            }

            long moola = (long) deposit.xanaxAmount() * moolaPerXanax;
            Instant tornEventTime = eventTimestamp > 0
                    ? Instant.ofEpochSecond(eventTimestamp)
                    : Instant.now();
            Deposit record = new Deposit(uniqueId, userTornId, "player", deposit.xanaxAmount(), moola, tornEventTime);
            record.setUser(user);
            depositRepository.save(record);

            totalMoola += moola;
            log.info("Matched deposit {} — {} xanax from tornId {} for user {}", uniqueId, deposit.xanaxAmount(), deposit.senderTornId(), userTornId);
        }

        return totalMoola;
    }
}
