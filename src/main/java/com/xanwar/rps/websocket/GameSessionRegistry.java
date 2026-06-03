package com.xanwar.rps.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xanwar.rps.model.GameRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(GameSessionRegistry.class);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToTornId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToUsername = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> tornIdToSessionIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> roomToSessionIds = new ConcurrentHashMap<>();

    public GameSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session) {
        sessionsById.put(session.getId(), session);
    }

    public void unregister(WebSocketSession session) {
        sessionsById.remove(session.getId());
        sessionToUsername.remove(session.getId());
        String tornId = sessionToTornId.remove(session.getId());
        if (tornId != null) {
            Set<String> sids = tornIdToSessionIds.get(tornId);
            if (sids != null) {
                sids.remove(session.getId());
                if (sids.isEmpty()) {
                    tornIdToSessionIds.remove(tornId);
                }
            }
        }
        roomToSessionIds.values().forEach(set -> set.remove(session.getId()));
    }

    public void registerIdentity(WebSocketSession session, String tornId, String username) {
        sessionToTornId.put(session.getId(), tornId);
        sessionToUsername.put(session.getId(), username);
        tornIdToSessionIds.computeIfAbsent(tornId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());
    }

    public void bindPlayer(WebSocketSession session, String tornId, String username, String roomId) {
        registerIdentity(session, tornId, username);
        bindToRoom(session, roomId);
    }

    public void bindToRoom(WebSocketSession session, String roomId) {
        roomToSessionIds.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet()).add(session.getId());
    }

    public String getTornId(WebSocketSession session) {
        return sessionToTornId.get(session.getId());
    }

    public void sendJson(WebSocketSession session, JsonNode payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    public void broadcastToAll(JsonNode payload) {
        sessionsById.values().forEach(session -> sendJson(session, payload));
    }

    public void sendToUser(String tornId, JsonNode payload) {
        Set<String> sids = tornIdToSessionIds.get(tornId);
        if (sids != null) {
            for (String sid : sids) {
                sendToSessionId(sid, payload);
            }
        }
    }

    public void sendToRoom(GameRoom room, JsonNode payload) {
        // Broadcast to all sessions explicitly bound to this room ID
        sendToRoomId(room.getRoomId(), payload);
        
        // ALSO broadcast to all sessions of the players involved (e.g. their lobby tabs)
        sendToUser(room.getPlayer1Id(), payload);
        if (room.getPlayer2Id() != null) {
            sendToUser(room.getPlayer2Id(), payload);
        }
    }

    public void sendToRoomId(String roomId, JsonNode payload) {
        Set<String> sessionIds = roomToSessionIds.get(roomId);
        if (sessionIds == null) {
            return;
        }
        for (String sessionId : sessionIds) {
            sendToSessionId(sessionId, payload);
        }
    }

    public void sendToSessionId(String sessionId, JsonNode payload) {
        if (sessionId == null) {
            return;
        }
        WebSocketSession session = sessionsById.get(sessionId);
        sendJson(session, payload);
    }

    public void removeRoom(String roomId) {
        roomToSessionIds.remove(roomId);
    }
}
