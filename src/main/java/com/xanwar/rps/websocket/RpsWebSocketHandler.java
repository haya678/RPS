package com.xanwar.rps.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xanwar.rps.service.ChatService;
import com.xanwar.rps.service.GameRoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RpsWebSocketHandler extends TextWebSocketHandler {

    private final GameRoomService gameRoomService;
    private final ChatService chatService;
    private final GameSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public RpsWebSocketHandler(
            GameRoomService gameRoomService,
            ChatService chatService,
            GameSessionRegistry sessionRegistry,
            ObjectMapper objectMapper
    ) {
        this.gameRoomService = gameRoomService;
        this.chatService = chatService;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tornId = sessionRegistry.getTornId(session);
        sessionRegistry.unregister(session);
        if (tornId != null) {
            gameRoomService.handleDisconnect(tornId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String action = json.path("action").asText();
            switch (action) {
                case "identify" -> chatService.identify(session, json);
                case "createRoom" -> gameRoomService.createRoom(session, json);
                case "joinRoom" -> gameRoomService.joinRoom(session, json);
                case "submitChoice" -> gameRoomService.submitChoice(session, json);
                case "listPublicRooms" -> gameRoomService.listPublicRooms(session);
                case "globalChat" -> chatService.sendGlobalChat(session, json);
                case "roomChat" -> chatService.sendRoomChat(session, json);
                default -> sendError(session, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
        } catch (Exception e) {
            sendError(session, "Server error: " + e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String error) {
        sessionRegistry.sendJson(session, objectMapper.createObjectNode()
                .put("action", "error")
                .put("message", error));
    }
}
