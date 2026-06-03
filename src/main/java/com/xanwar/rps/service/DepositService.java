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

import com.xanwar.rps.util.ApiResponse;

import java.time.Instant;
import java.util.ArrayList;
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
            return ApiResponse.error("Could not read house Torn events. Check TORN_API_MY_KEY has full access (info, events, log). Error: " + e.getMessage());
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
            Map<String, Object> result = ApiResponse.success();
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
        Map<String, Object> result = ApiResponse.success();
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
        if (node == null || node.isMissingNode() || node.isNull() || (!node.isObject() && !node.isArray())) {
            log.debug("[{}] No node to process (missing/null/not-activity-list)", source);
            return 0L;
        }

        long totalMoola = 0L;
        int processedCount = 0;
        int skippedOld = 0;
        int skippedNoText = 0;
        int skippedNoMatch = 0;
        int skippedWrongUser = 0;
        int skippedDuplicate = 0;

        for (ActivityEntry entry : activityEntries(node)) {
            String eventId = entry.id();
            JsonNode item = entry.item();

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

            String fallbackSenderId = extractSenderTornId(item);
            List<String> eventTexts = extractActivityTexts(item);
            Optional<ParsedDeposit> parsed = parseStructuredDeposit(item, requiredMessage, fallbackSenderId);

            if (parsed.isEmpty()) {
                for (String eventText : eventTexts) {
                    // Only look at entries mentioning Xanax to avoid unnecessary parsing
                    if (!eventText.toLowerCase().contains("xanax")) {
                        continue;
                    }

                    log.debug("[{}] Event {}: {}", source, eventId,
                            eventText.length() > 120 ? eventText.substring(0, 120) + "..." : eventText);

                    parsed = TornXanaxDepositParser.parse(eventText, requiredMessage, fallbackSenderId);
                    if (parsed.isPresent()) {
                        break;
                    }
                }
            }
            if (parsed.isEmpty()) {
                String separateMessage = extractTransferMessage(item, eventTexts);
                for (String eventText : eventTexts) {
                    if (!eventText.toLowerCase().contains("xanax")) {
                        continue;
                    }
                    parsed = TornXanaxDepositParser.parseTransfer(
                            eventText, separateMessage, requiredMessage, fallbackSenderId);
                    if (parsed.isPresent()) {
                        break;
                    }
                }
            }

            if (eventTexts.isEmpty() && parsed.isEmpty()) {
                skippedNoText++;
                continue;
            }

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

            String uniqueId = source + "-" + eventId;
            String legacyUniqueId = "ev-" + eventId;
            if (depositRepository.existsByEventId(uniqueId) || depositRepository.existsByEventId(legacyUniqueId)) {
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

    private List<ActivityEntry> activityEntries(JsonNode node) {
        List<ActivityEntry> entries = new ArrayList<>();
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                entries.add(new ActivityEntry(field.getKey(), field.getValue()));
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode item = node.get(i);
                String id = firstText(item.path("id").asText(""), item.path("event_id").asText(""));
                entries.add(new ActivityEntry(id.isBlank() ? String.valueOf(i) : id, item));
            }
        }
        return entries;
    }

    private List<String> extractActivityTexts(JsonNode item) {
        List<String> texts = new ArrayList<>();
        addText(texts, item.path("event").asText(""));
        addText(texts, item.path("log").asText(""));
        addText(texts, item.path("title").asText(""));
        addText(texts, item.path("message").asText(""));
        addText(texts, item.path("description").asText(""));
        collectTextFields(item.path("data"), texts);
        return texts.stream().distinct().toList();
    }

    private void collectTextFields(JsonNode node, List<String> texts) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            addText(texts, node.asText(""));
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(field -> collectTextFields(field.getValue(), texts));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectTextFields(child, texts));
        }
    }

    private void addText(List<String> texts, String text) {
        if (text != null && !text.isBlank()) {
            texts.add(text.trim());
        }
    }

    private Optional<ParsedDeposit> parseStructuredDeposit(JsonNode item, String requiredMessage, String fallbackSenderId) {
        JsonNode data = item.path("data");
        if (!data.isObject()) {
            return Optional.empty();
        }

        String senderId = firstText(
                fallbackSenderId,
                data.path("sender_id").asText(""),
                data.path("senderId").asText(""),
                data.path("from_id").asText(""),
                data.path("fromId").asText(""),
                data.path("user_id").asText(""),
                data.path("userId").asText(""),
                data.path("sender").path("id").asText(""),
                data.path("from").path("id").asText(""),
                data.path("user").path("id").asText(""),
                scalarNumericId(data.path("sender")),
                scalarNumericId(data.path("from"))
        );
        if (senderId.isBlank()) {
            return Optional.empty();
        }

        String itemName = firstText(
                data.path("item").asText(""),
                data.path("item_name").asText(""),
                data.path("itemName").asText(""),
                data.path("name").asText(""),
                data.path("object").asText("")
        );
        if (!itemName.toLowerCase().contains("xanax")) {
            itemName = extractItemNameFromArray(data);
            if (!itemName.toLowerCase().contains("xanax")) {
                return Optional.empty();
            }
        }

        String message = firstText(
                data.path("message").asText(""),
                data.path("msg").asText(""),
                data.path("note").asText(""),
                data.path("memo").asText("")
        );
        if (!message.equalsIgnoreCase(requiredMessage.trim())) {
            return Optional.empty();
        }

        int amount = firstPositiveInt(
                data.path("quantity"),
                data.path("qty"),
                data.path("amount"),
                data.path("count")
        );
        if (amount <= 0) {
            amount = extractAmountFromItemsArray(data);
        }
        if (amount <= 0) {
            return Optional.empty();
        }

        return Optional.of(new ParsedDeposit(amount, senderId, message));
    }

    /** Extracts item name from Torn's array format: "items": [{"name": "Xanax", ...}] */
    private String extractItemNameFromArray(JsonNode data) {
        JsonNode items = data.path("items");
        if (!items.isArray()) {
            return "";
        }
        for (JsonNode entry : items) {
            String name = firstText(
                    entry.path("name").asText(""),
                    entry.path("item_name").asText(""),
                    entry.path("itemName").asText("")
            );
            if (!name.isBlank()) {
                return name;
            }
        }
        return "";
    }

    /** Extracts quantity from Torn's items array: "items": [{"qty": 1, ...}] */
    private int extractAmountFromItemsArray(JsonNode data) {
        JsonNode items = data.path("items");
        if (!items.isArray()) {
            return 0;
        }
        int total = 0;
        for (JsonNode entry : items) {
            String name = firstText(
                    entry.path("name").asText(""),
                    entry.path("item_name").asText(""),
                    entry.path("itemName").asText("")
            );
            if (name.toLowerCase().contains("xanax")) {
                int qty = firstPositiveInt(
                        entry.path("quantity"),
                        entry.path("qty"),
                        entry.path("amount"),
                        entry.path("count")
                );
                if (qty > 0) {
                    total += qty;
                }
            }
        }
        return total;
    }

    private String extractTransferMessage(JsonNode item, List<String> eventTexts) {
        JsonNode data = item.path("data");
        String structured = firstText(
                item.path("message").asText(""),
                item.path("msg").asText(""),
                item.path("note").asText(""),
                item.path("memo").asText(""),
                data.path("message").asText(""),
                data.path("msg").asText(""),
                data.path("note").asText(""),
                data.path("memo").asText("")
        );
        if (!structured.isBlank()) {
            return structured;
        }

        for (String text : eventTexts) {
            String stripped = TornXanaxDepositParser.stripHtml(text).trim();
            if (!stripped.toLowerCase().contains("message")) {
                continue;
            }
            int colon = stripped.indexOf(':');
            if (colon >= 0 && colon + 1 < stripped.length()) {
                return stripped.substring(colon + 1).trim();
            }
        }
        return "";
    }

    private String extractSenderTornId(JsonNode item) {
        return firstText(
                item.path("sender_id").asText(""),
                item.path("senderId").asText(""),
                item.path("from_id").asText(""),
                item.path("fromId").asText(""),
                item.path("user_id").asText(""),
                item.path("userId").asText(""),
                item.path("sender").path("id").asText(""),
                item.path("from").path("id").asText(""),
                item.path("user").path("id").asText(""),
                scalarNumericId(item.path("sender")),
                scalarNumericId(item.path("from")),
                item.path("data").path("sender_id").asText(""),
                item.path("data").path("senderId").asText(""),
                item.path("data").path("from_id").asText(""),
                item.path("data").path("fromId").asText(""),
                item.path("data").path("user_id").asText(""),
                item.path("data").path("userId").asText(""),
                item.path("data").path("sender").path("id").asText(""),
                item.path("data").path("from").path("id").asText(""),
                item.path("data").path("user").path("id").asText(""),
                scalarNumericId(item.path("data").path("sender")),
                scalarNumericId(item.path("data").path("from"))
        );
    }

    /** Extracts a numeric ID from a scalar node (int/long), ignoring objects and missing nodes. */
    private String scalarNumericId(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.isObject() || node.isArray()) {
            return "";
        }
        if (node.isNumber()) {
            long val = node.asLong(0);
            return val > 0 ? String.valueOf(val) : "";
        }
        String text = node.asText("").trim();
        if (!text.isEmpty()) {
            try {
                long val = Long.parseLong(text);
                return val > 0 ? String.valueOf(val) : "";
            } catch (NumberFormatException ignored) {
                return "";
            }
        }
        return "";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private int firstPositiveInt(JsonNode... values) {
        for (JsonNode value : values) {
            int parsed = value.asInt(0);
            if (parsed > 0) {
                return parsed;
            }
            if (value.isTextual()) {
                try {
                    parsed = Integer.parseInt(value.asText("").trim());
                    if (parsed > 0) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // Try the next shape Torn may return.
                }
            }
        }
        return 0;
    }

    private record ActivityEntry(String id, JsonNode item) {
    }
}
