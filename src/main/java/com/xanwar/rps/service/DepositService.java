package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.config.TornDepositProperties;
import com.xanwar.rps.model.Deposit;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.DepositRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DepositService {

    private final UserService userService;
    private final DepositRepository depositRepository;
    private final TornApiClient tornApiClient;
    private final TornDepositProperties depositProperties;
    private final GameProperties gameProperties;
    private final Pattern xanaxEventPattern;

    public DepositService(
            UserService userService,
            DepositRepository depositRepository,
            TornApiClient tornApiClient,
            TornDepositProperties depositProperties,
            GameProperties gameProperties
    ) {
        this.userService = userService;
        this.depositRepository = depositRepository;
        this.tornApiClient = tornApiClient;
        this.depositProperties = depositProperties;
        this.gameProperties = gameProperties;
        String message = Pattern.quote(depositProperties.getRequiredMessage());
        this.xanaxEventPattern = Pattern.compile(
                "You received (\\d+)x Xanax from (.+?) \\((\\d+)\\) with the message: " + message
        );
    }

    @Transactional
    public Map<String, Object> verifyDeposit(String userTornId) {
        Map<String, Object> result = new HashMap<>();
        User user = userService.requireUser(userTornId);

        JsonNode data = tornApiClient.fetchHouseEvents();
        JsonNode events = data.path("events");
        if (events.isMissingNode() || events.isNull() || events.isEmpty()) {
            return successNoDeposits(user, result);
        }

        long cutoffEpoch = Instant.now().getEpochSecond()
                - (depositProperties.getMaxAgeHours() * 3600L);
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
            if (eventTimestamp < cutoffEpoch) {
                continue;
            }

            String eventText = event.path("event").asText("");
            Matcher matcher = xanaxEventPattern.matcher(eventText);
            if (!matcher.matches()) {
                continue;
            }

            int amount = Integer.parseInt(matcher.group(1));
            String senderName = matcher.group(2);
            String senderTornId = matcher.group(3);

            if (!senderTornId.equals(userTornId)) {
                continue;
            }
            if (depositRepository.existsByEventId(eventId)) {
                continue;
            }

            long moola = (long) amount * moolaPerXanax;
            Instant tornEventTime = Instant.ofEpochSecond(eventTimestamp);
            Deposit deposit = new Deposit(eventId, userTornId, senderName, amount, moola, tornEventTime);
            deposit.setUser(user);
            depositRepository.save(deposit);

            totalMoola += moola;
            processedCount++;
        }

        if (processedCount == 0) {
            result.put("success", true);
            result.put("message",
                    "No new deposits found. Send Xanax with message '"
                            + depositProperties.getRequiredMessage()
                            + "' and try again.");
            result.put("site_balance", user.getSiteBalance());
            result.put("new_moola", 0);
            return result;
        }

        user.setSiteBalance(user.getSiteBalance() + totalMoola);
        result.put("success", true);
        result.put("message", "Verified " + processedCount + " deposit(s): "
                + (totalMoola / moolaPerXanax) + " Xanax (" + totalMoola + " Moola)");
        result.put("site_balance", user.getSiteBalance());
        result.put("new_moola", totalMoola);
        return result;
    }

    private Map<String, Object> successNoDeposits(User user, Map<String, Object> result) {
        result.put("success", true);
        result.put("message", "No events found.");
        result.put("site_balance", user.getSiteBalance());
        result.put("new_moola", 0);
        return result;
    }
}
