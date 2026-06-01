package com.xanwar.rps.game;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Torn event/log lines for incoming Xanax with a transfer message.
 */
public final class TornXanaxDepositParser {

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile(
                    "You received (\\d+)x Xanax from .*?(?:\\[|\\()(\\d+)(?:\\]|\\))\\s*with the message:\\s*"
                            + "(.+?)\\.?\\s*$",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "You were sent (\\d+)x Xanax by .*?(?:\\[|\\()(\\d+)(?:\\]|\\))\\s*with the message:\\s*"
                            + "(.+?)\\.?\\s*$",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    ".*?\\[(\\d+)]\\s*sent you (\\d+)x Xanax with the message:\\s*(.+?)\\.?\\s*$",
                    Pattern.CASE_INSENSITIVE)
    );

    private TornXanaxDepositParser() {
    }

    public record ParsedDeposit(int xanaxAmount, String senderTornId, String messageText) {
    }

    public static Optional<ParsedDeposit> parse(String rawText, String requiredMessage) {
        if (rawText == null || rawText.isBlank() || requiredMessage == null || requiredMessage.isBlank()) {
            return Optional.empty();
        }
        String text = stripHtml(rawText).replaceAll("\\s+", " ").trim();
        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            int amount;
            String senderId;
            String messageText;
            if (pattern.pattern().startsWith(".*?\\[")) {
                senderId = matcher.group(1);
                amount = Integer.parseInt(matcher.group(2));
                messageText = matcher.group(3);
            } else {
                amount = Integer.parseInt(matcher.group(1));
                senderId = matcher.group(2);
                messageText = matcher.group(3);
            }
            messageText = normalizeMessage(messageText);
            if (!messageMatches(messageText, requiredMessage)) {
                continue;
            }
            return Optional.of(new ParsedDeposit(amount, senderId, messageText));
        }
        return Optional.empty();
    }

    static String stripHtml(String raw) {
        return raw.replaceAll("<[^>]+>", "").replace("&nbsp;", " ");
    }

    static String normalizeMessage(String message) {
        String m = message.trim();
        if ((m.startsWith("\"") && m.endsWith("\"")) || (m.startsWith("'") && m.endsWith("'"))) {
            m = m.substring(1, m.length() - 1).trim();
        }
        return m;
    }

    static boolean messageMatches(String actual, String required) {
        return actual.trim().equalsIgnoreCase(required.trim());
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
