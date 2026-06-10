package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xanwar.rps.model.GameRoom;
import com.xanwar.rps.model.RoomStatus;
import com.xanwar.rps.websocket.GameSessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.xanwar.rps.util.WebSocketMessages;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final long MIN_MESSAGE_INTERVAL_MS = 2500; // Increased to 2.5s for spam protection

    private static final java.util.List<String> BANNED_WORDS = java.util.Arrays.asList(
        "fuck", "shit", "bitch", "cunt", "asshole", "faggot", "nigger", "dick", "pussy", "dumbass", "idiot"
    );

    private final GameRoomService gameRoomService;
    private final GameSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> lastMessageAtByTornId = new ConcurrentHashMap<>();

    public ChatService(
            GameRoomService gameRoomService,
            GameSessionRegistry sessionRegistry,
            ObjectMapper objectMapper
    ) {
        this.gameRoomService = gameRoomService;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void identify(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String username = requiredText(json, "username");
        sessionRegistry.registerIdentity(session, tornId, username);

        ObjectNode ack = objectMapper.createObjectNode();
        ack.put("action", "identified");
        ack.put("tornId", tornId);
        ack.put("username", username);
        sessionRegistry.sendJson(session, ack);
    }

    public void sendGlobalChat(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String username = requiredText(json, "username");
        String message = sanitizeMessage(requiredText(json, "message"));

        sessionRegistry.registerIdentity(session, tornId, username);
        if (!allowMessage(tornId)) {
            sendChatError(session, "Slow down! Chatting too fast.");
            return;
        }

        String filteredMessage = filterProfanity(message);
        ObjectNode payload = buildChatPayload("global", null, tornId, username, filteredMessage);
        sessionRegistry.broadcastToAll(payload);
    }

    public void sendRoomChat(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String username = requiredText(json, "username");
        String roomId = requiredText(json, "roomId");
        String message = sanitizeMessage(requiredText(json, "message"));

        GameRoom room = gameRoomService.getRoom(roomId);
        if (room == null) {
            sendChatError(session, "Room not found.");
            return;
        }
        if (!canUseRoomChat(room, tornId)) {
            sendChatError(session, "You must be in this room to chat.");
            return;
        }

        sessionRegistry.registerIdentity(session, tornId, username);
        sessionRegistry.bindToRoom(session, roomId);

        if (!allowMessage(tornId)) {
            sendChatError(session, "Slow down! Chatting too fast.");
            return;
        }

        String filteredMessage = filterProfanity(message);
        ObjectNode payload = buildChatPayload("room", roomId, tornId, username, filteredMessage);
        sessionRegistry.sendToRoomId(roomId, payload);
    }

    private String filterProfanity(String message) {
        String filtered = message;
        for (String word : BANNED_WORDS) {
            // Case-insensitive regex for whole words
            String regex = "(?i)\\b" + java.util.regex.Pattern.quote(word) + "\\b";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(filtered);
            
            StringBuilder sb = new StringBuilder();
            int lastIndex = 0;
            while (matcher.find()) {
                sb.append(filtered, lastIndex, matcher.start());
                sb.append("<span class=\"blurred-text\">").append(matcher.group()).append("</span>");
                lastIndex = matcher.end();
            }
            sb.append(filtered.substring(lastIndex));
            filtered = sb.toString();
        }
        return filtered;
    }

    private boolean canUseRoomChat(GameRoom room, String tornId) {
        if (room.involvesPlayer(tornId)) {
            return true;
        }
        return room.getStatus() == RoomStatus.WAITING && room.getPlayer1Id().equals(tornId);
    }

    private ObjectNode buildChatPayload(String scope, String roomId, String tornId, String username, String message) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("action", "chatMessage");
        payload.put("scope", scope);
        if (roomId != null) {
            payload.put("roomId", roomId);
        }
        payload.put("tornId", tornId);
        payload.put("username", username);
        payload.put("message", message);
        payload.put("timestamp", Instant.now().toEpochMilli());
        return payload;
    }

    private boolean allowMessage(String tornId) {
        long now = System.currentTimeMillis();
        Long last = lastMessageAtByTornId.put(tornId, now);
        return last == null || now - last >= MIN_MESSAGE_INTERVAL_MS;
    }

    private String sanitizeMessage(String raw) {
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            return trimmed.substring(0, MAX_MESSAGE_LENGTH);
        }
        return trimmed;
    }

    private String requiredText(JsonNode json, String field) {
        return WebSocketMessages.requiredText(json, field);
    }

    private void sendChatError(WebSocketSession session, String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("action", "chatError");
        node.put("message", message);
        sessionRegistry.sendJson(session, node);
    }
}
