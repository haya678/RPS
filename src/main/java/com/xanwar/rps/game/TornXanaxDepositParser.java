package com.xanwar.rps.game;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Torn event-log lines for incoming Xanax with a transfer message.
 * Torn uses square brackets for player IDs, e.g. {@code Playername [3961385]}.
 */
public final class TornXanaxDepositParser {

    /**
     * Handles HTML links in names, [id] or (id), optional quotes, trailing period.
     */
    private static final Pattern XANAX_RECEIVED = Pattern.compile(
            "You received (\\d+)x Xanax from .*?(?:\\[|\\()(\\d+)(?:\\]|\\))\\s*with the message:\\s*"
                    + "(?:[\"'])?(.+?)(?:[\"'])?\\.?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private TornXanaxDepositParser() {
    }

    public record ParsedDeposit(int xanaxAmount, String senderTornId, String messageText) {
    }

    public static Optional<ParsedDeposit> parse(String eventText, String requiredMessage) {
        if (eventText == null || eventText.isBlank() || requiredMessage == null || requiredMessage.isBlank()) {
            return Optional.empty();
        }
        String normalized = eventText.trim().replaceAll("\\s+", " ");
        Matcher matcher = XANAX_RECEIVED.matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String messageText = matcher.group(3).trim();
        if (!messageMatches(messageText, requiredMessage)) {
            return Optional.empty();
        }
        int amount = Integer.parseInt(matcher.group(1));
        String senderId = matcher.group(2);
        return Optional.of(new ParsedDeposit(amount, senderId, messageText));
    }

    static boolean messageMatches(String actual, String required) {
        String a = actual.trim();
        String r = required.trim();
        return a.equalsIgnoreCase(r);
    }

    public static boolean tornIdsMatch(String eventTornId, String userTornId) {
        if (eventTornId == null || userTornId == null) {
            return false;
        }
        if (eventTornId.equals(userTornId)) {
            return true;
        }
        try {
            return Integer.parseInt(eventTornId) == Integer.parseInt(userTornId);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
