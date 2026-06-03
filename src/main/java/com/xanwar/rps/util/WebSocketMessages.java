package com.xanwar.rps.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xanwar.rps.websocket.GameSessionRegistry;
import org.springframework.web.socket.WebSocketSession;

public final class WebSocketMessages {

    private WebSocketMessages() {}

    public static String requiredText(JsonNode json, String field) {
        JsonNode node = json.path(field);
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        String value = node.asText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return value;
    }

    public static void sendError(
            GameSessionRegistry registry,
            ObjectMapper mapper,
            WebSocketSession session,
            String message
    ) {
        ObjectNode node = mapper.createObjectNode();
        node.put("action", "error");
        node.put("message", message);
        registry.sendJson(session, node);
    }
}
