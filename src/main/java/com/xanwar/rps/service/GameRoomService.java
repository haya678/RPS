package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.game.PotSettlement;
import com.xanwar.rps.game.RpsRules;
import com.xanwar.rps.model.GameRoom;
import com.xanwar.rps.model.RoomStatus;
import com.xanwar.rps.model.RoomVisibility;
import com.xanwar.rps.websocket.GameSessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.xanwar.rps.util.WebSocketMessages;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRoomService {

    private final WalletService walletService;
    private final UserService userService;
    private final GameProperties gameProperties;
    private final GameSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2);

    public GameRoomService(
            WalletService walletService,
            UserService userService,
            GameProperties gameProperties,
            GameSessionRegistry sessionRegistry,
            ObjectMapper objectMapper
    ) {
        this.walletService = walletService;
        this.userService = userService;
        this.gameProperties = gameProperties;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void createRoom(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String username = requiredText(json, "username");
        long betAmount = json.path("betAmount").asLong();

        if (!isValidBet(betAmount)) {
            sendError(session, "Bet must be at least " + gameProperties.getMinBetMoola()
                    + " and a multiple of " + gameProperties.getMoolaPerXanax() + " Moola.");
            return;
        }

        if (hasActiveMatch(tornId)) {
            sendError(session, "You already have an active room or match in progress.");
            return;
        }

        int rounds = json.path("rounds").asInt(3);
        if (rounds < 1 || rounds > 99 || rounds % 2 == 0) {
            sendError(session, "Rounds must be an odd number (e.g. 1, 3, 5, 7).");
            return;
        }

        if (!walletService.deductBalance(tornId, betAmount)) {
            sendError(session, "Insufficient balance.");
            return;
        }
        userService.recordBet(tornId, betAmount);

        boolean isPublic = json.path("isPublic").asBoolean(false);
        boolean playWithBot = json.path("playWithBot").asBoolean(false);
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        int winsRequired = (rounds + 1) / 2;
        RoomVisibility visibility = isPublic ? RoomVisibility.PUBLIC : RoomVisibility.PRIVATE;
        if (playWithBot) {
            visibility = RoomVisibility.PRIVATE;
        }
        GameRoom room = new GameRoom(roomId, tornId, username, betAmount, winsRequired, session.getId(), visibility);
        rooms.put(roomId, room);
        sessionRegistry.bindPlayer(session, tornId, username, roomId);

        if (playWithBot) {
            room.tryStartMatch("BOT_BAINING", "The House", "BOT_SESSION");
            sessionRegistry.sendJson(session, buildMatchStartedPayload(room));
            startRoundTimer(room);
        } else {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("action", "roomCreated");
            response.put("roomId", roomId);
            response.put("betAmount", betAmount);
            response.put("isPublic", isPublic);
            response.put("rounds", rounds);
            response.put("winsRequired", winsRequired);
            response.put("status", RoomStatus.WAITING.name());
            sessionRegistry.sendJson(session, response);
            broadcastPublicRooms();
        }
    }

    public void listPublicRooms(WebSocketSession session) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("action", "publicRooms");
        response.set("rooms", buildPublicRoomsArray());
        sessionRegistry.sendJson(session, response);
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<GameRoom> listPublicWaitingRooms() {
        return rooms.values().stream()
                .filter(room -> room.isPublic() && room.getStatus() == RoomStatus.WAITING)
                .sorted(Comparator.comparingLong(GameRoom::getCreatedAtEpochMs).reversed())
                .toList();
    }

    public void broadcastPublicRooms() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("action", "publicRooms");
        response.set("rooms", buildPublicRoomsArray());
        sessionRegistry.broadcastToAll(response);
    }

    private ArrayNode buildPublicRoomsArray() {
        ArrayNode array = objectMapper.createArrayNode();
        listPublicWaitingRooms().forEach(room -> {
            ObjectNode row = objectMapper.createObjectNode();
            row.put("roomId", room.getRoomId());
            row.put("host", room.getPlayer1Name());
            row.put("hostId", room.getPlayer1Id());
            row.put("betAmount", room.getBetAmount());
            row.put("winsRequired", room.getWinsRequired());
            row.put("rounds", room.getWinsRequired() * 2 - 1);
            row.put("isPublic", true);
            array.add(row);
        });
        return array;
    }

    public void joinRoom(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String username = requiredText(json, "username");
        String roomId = requiredText(json, "roomId");

        if (hasActiveMatch(tornId)) {
            sendError(session, "You already have an active room or match in progress.");
            return;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null) {
            sendError(session, "Room not found.");
            return;
        }

        synchronized (room) {
            if (room.getStatus() != RoomStatus.WAITING) {
                sendError(session, "Room is not available.");
                return;
            }
            if (room.getPlayer1Id().equals(tornId)) {
                sendError(session, "Cannot join your own room.");
                return;
            }
        }

        if (!walletService.deductBalance(tornId, room.getBetAmount())) {
            sendError(session, "Insufficient balance.");
            return;
        }
        userService.recordBet(tornId, room.getBetAmount());

        boolean started;
        synchronized (room) {
            started = room.tryStartMatch(tornId, username, session.getId());
        }

        if (!started) {
            walletService.refundBalance(tornId, room.getBetAmount());
            sendError(session, "Room is no longer available.");
            return;
        }

        sessionRegistry.bindPlayer(session, tornId, username, roomId);

        sessionRegistry.sendToRoom(room, buildMatchStartedPayload(room));
        broadcastPublicRooms();
        startRoundTimer(room);
    }

    public void cancelRoom(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String roomId = json.path("roomId").asText("").trim();

        GameRoom room = roomId.isEmpty()
                ? findWaitingRoomHostedBy(tornId)
                : rooms.get(roomId);

        if (room == null) {
            sendError(session, "Room not found.");
            return;
        }

        synchronized (room) {
            if (!tornId.equals(room.getPlayer1Id())) {
                sendError(session, "Only the host can cancel this room.");
                return;
            }
            if (room.getStatus() != RoomStatus.WAITING) {
                sendError(session, "This room is no longer waiting for players.");
                return;
            }
        }

        walletService.refundBalance(room.getPlayer1Id(), room.getBetAmount());

        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("action", "roomCancelled");
        msg.put("roomId", room.getRoomId());
        msg.put("message", "Room cancelled. Your bet was refunded.");
        sessionRegistry.sendJson(session, msg);
        cleanupRoom(room);
    }

    public void forfeitMatch(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String roomId = requiredText(json, "roomId");

        GameRoom room = rooms.get(roomId);
        if (room == null) {
            sendError(session, "Room not found.");
            return;
        }

        synchronized (room) {
            if (room.getStatus() != RoomStatus.IN_PROGRESS) {
                sendError(session, "No match in progress to forfeit.");
                return;
            }
            if (!room.involvesPlayer(tornId)) {
                sendError(session, "You are not a participant in this match.");
                return;
            }
        }

        // Determine scores to make the other player win
        int winsRequired = room.getWinsRequired();
        int p1Wins, p2Wins;
        if (tornId.equals(room.getPlayer1Id())) {
            p1Wins = room.getPlayer1Wins();
            p2Wins = winsRequired; // Player 2 wins
        } else {
            p1Wins = winsRequired; // Player 1 wins
            p2Wins = room.getPlayer2Wins();
        }

        ObjectNode forfeitMsg = objectMapper.createObjectNode();
        forfeitMsg.put("action", "chatMessage");
        forfeitMsg.put("scope", "room");
        forfeitMsg.put("roomId", room.getRoomId());
        forfeitMsg.put("tornId", "SYSTEM");
        forfeitMsg.put("username", "Match Info");
        String forfeiter = tornId.equals(room.getPlayer1Id()) ? room.getPlayer1Name() : room.getPlayer2Name();
        forfeitMsg.put("message", forfeiter + " has forfeited the match.");
        forfeitMsg.put("timestamp", System.currentTimeMillis());
        sessionRegistry.sendToRoom(room, forfeitMsg);

        cancelRoundTimer(room.getRoomId());
        finishMatch(room, p1Wins, p2Wins);
    }

    public void submitChoice(WebSocketSession session, JsonNode json) {
        String tornId = requiredText(json, "tornId");
        String roomId = requiredText(json, "roomId");
        String choice = requiredText(json, "choice").toLowerCase();

        if (!RpsRules.isValidChoice(choice)) {
            sendError(session, "Invalid choice. Must be rock, paper, or scissors.");
            return;
        }

        GameRoom room = rooms.get(roomId);
        if (room == null) {
            sendError(session, "Room not found.");
            return;
        }

        boolean recorded;
        synchronized (room) {
            if (room.getStatus() != RoomStatus.IN_PROGRESS) {
                sendError(session, "Game not in progress.");
                return;
            }
            if (!room.involvesPlayer(tornId)) {
                sendError(session, "You are not in this game.");
                return;
            }
            recorded = room.recordChoice(tornId, choice);
        }

        if (!recorded) {
            sendError(session, "Choice already submitted for this round.");
            return;
        }

        ObjectNode ack = objectMapper.createObjectNode();
        ack.put("action", "choiceReceived");
        sessionRegistry.sendJson(session, ack);

        if ("BOT_BAINING".equals(room.getPlayer2Id())) {
            String[] options = {"rock", "paper", "scissors"};
            String botChoice = options[(int) (Math.random() * 3)];
            room.recordChoice("BOT_BAINING", botChoice);
            evaluateRound(room);
        } else {
            boolean evaluate;
            synchronized (room) {
                evaluate = room.bothChoicesSubmitted();
            }
            if (evaluate) {
                evaluateRound(room);
            } else {
                String opponentSessionId = tornId.equals(room.getPlayer1Id()) ? room.getPlayer2SessionId() : room.getPlayer1SessionId();
                if (opponentSessionId != null) {
                    ObjectNode oppNode = objectMapper.createObjectNode();
                    oppNode.put("action", "opponentSelected");
                    sessionRegistry.sendToSessionId(opponentSessionId, oppNode);
                }
            }
        }
    }

    public void handleDisconnect(String tornId) {
        for (Map.Entry<String, GameRoom> entry : rooms.entrySet()) {
            GameRoom room = entry.getValue();
            if (!room.involvesPlayer(tornId)) {
                continue;
            }

            synchronized (room) {
                if (room.getStatus() == RoomStatus.WAITING && tornId.equals(room.getPlayer1Id())) {
                    walletService.refundBalance(room.getPlayer1Id(), room.getBetAmount());
                    ObjectNode msg = objectMapper.createObjectNode();
                    msg.put("action", "roomCancelled");
                    msg.put("roomId", room.getRoomId());
                    msg.put("message", "Host disconnected. Bet refunded.");
                    sessionRegistry.sendToSessionId(room.getPlayer1SessionId(), msg);
                    cleanupRoom(room);
                    return;
                }

                if (room.getStatus() == RoomStatus.IN_PROGRESS) {
                    String oppSessionId = tornId.equals(room.getPlayer1Id()) ? room.getPlayer2SessionId() : room.getPlayer1SessionId();
                    if (oppSessionId != null) {
                        ObjectNode note = objectMapper.createObjectNode();
                        note.put("action", "opponentConnectionStatus");
                        note.put("connected", false);
                        sessionRegistry.sendToSessionId(oppSessionId, note);
                    }
                    return;
                }
            }
            return;
        }
    }

    private void evaluateRound(GameRoom room) {
        String p1Choice;
        String p2Choice;
        int roundNumber;
        int p1Wins;
        int p2Wins;
        int roundOutcome;

        synchronized (room) {
            p1Choice = room.getPlayer1Choice();
            p2Choice = room.getPlayer2Choice();
            roundNumber = room.getCurrentRound();
            roundOutcome = RpsRules.roundWinner(p1Choice, p2Choice);

            if (roundOutcome == 1) {
                room.setPlayer1Wins(room.getPlayer1Wins() + 1);
            } else if (roundOutcome == 2) {
                room.setPlayer2Wins(room.getPlayer2Wins() + 1);
            }

            p1Wins = room.getPlayer1Wins();
            p2Wins = room.getPlayer2Wins();
            room.clearRoundChoices();
        }

        ObjectNode roundResult = objectMapper.createObjectNode();
        roundResult.put("action", "roundResult");
        roundResult.put("round", roundNumber);
        roundResult.put("player1Choice", p1Choice);
        roundResult.put("player2Choice", p2Choice);
        roundResult.put("player1", room.getPlayer1Name());
        roundResult.put("player2", room.getPlayer2Name());
        roundResult.put("player1Wins", p1Wins);
        roundResult.put("player2Wins", p2Wins);

        if (roundOutcome == 0) {
            roundResult.put("roundWinner", "tie");
        } else if (roundOutcome == 1) {
            roundResult.put("roundWinner", room.getPlayer1Name());
        } else {
            roundResult.put("roundWinner", room.getPlayer2Name());
        }

        boolean matchOver;
        synchronized (room) {
            matchOver = room.isMatchOver();
            if (!matchOver) {
                room.setCurrentRound(room.getCurrentRound() + 1);
                roundResult.put("nextRound", room.getCurrentRound());
            }
        }

        roundResult.put("matchOver", matchOver);
        roundResult.put("winsRequired", room.getWinsRequired());
        roundResult.put("betAmount", room.getBetAmount());
        sessionRegistry.sendToRoom(room, roundResult);

        if ("BOT_BAINING".equals(room.getPlayer2Id())) {
            triggerBotChat(room, p1Choice, p2Choice, roundOutcome);
        }

        if (matchOver) {
            cancelRoundTimer(room.getRoomId());
            finishMatch(room, p1Wins, p2Wins);
        } else {
            startRoundTimer(room);
        }
    }

    private void finishMatch(GameRoom room, int p1Wins, int p2Wins) {
        String winnerId;
        String winnerName;
        if (p1Wins >= room.getWinsRequired()) {
            winnerId = room.getPlayer1Id();
            winnerName = room.getPlayer1Name();
        } else {
            winnerId = room.getPlayer2Id();
            winnerName = room.getPlayer2Name();
        }

        PotSettlement settlement = PotSettlement.fromPot(room.getPot(), gameProperties.getHouseRakePercent());
        if (!"BOT_BAINING".equals(winnerId)) {
            walletService.creditBalance(winnerId, settlement.winnerPayout());
        }
        String loserId = winnerId.equals(room.getPlayer1Id()) ? room.getPlayer2Id() : room.getPlayer1Id();
        String loserName = winnerId.equals(room.getPlayer1Id()) ? room.getPlayer2Name() : room.getPlayer1Name();
        userService.recordMatchOutcome(winnerId, loserId, settlement.winnerPayout(), room.getBetAmount());

        synchronized (room) {
            room.setStatus(RoomStatus.FINISHED);
        }

        ObjectNode matchEnd = objectMapper.createObjectNode();
        matchEnd.put("action", "matchEnd");
        matchEnd.put("winner", winnerName);
        matchEnd.put("winnerId", winnerId);
        matchEnd.put("pot", settlement.pot());
        matchEnd.put("rake", settlement.rake());
        matchEnd.put("winnings", settlement.winnerPayout());
        matchEnd.put("player1Wins", p1Wins);
        matchEnd.put("player2Wins", p2Wins);
        matchEnd.put("winsRequired", room.getWinsRequired());
        matchEnd.put("betAmount", room.getBetAmount());
        matchEnd.put("player1", room.getPlayer1Name());
        matchEnd.put("player2", room.getPlayer2Name());

        sessionRegistry.sendToRoom(room, matchEnd);

        if (settlement.winnerPayout() >= 100L) {
            ObjectNode announce = objectMapper.createObjectNode();
            announce.put("action", "chatMessage");
            announce.put("scope", "global");
            announce.put("tornId", "SYSTEM");
            announce.put("username", "🏆 ANNOUNCEMENT");
            int totalRounds = room.getWinsRequired() * 2 - 1;
            announce.put("message", winnerName + " won a massive match against " + loserName + " taking home " + settlement.winnerPayout() + " Moola (Best of " + totalRounds + ")!");
            announce.put("timestamp", System.currentTimeMillis());
            sessionRegistry.broadcastToAll(announce);
        }

        cleanupRoom(room);
    }

    private GameRoom findWaitingRoomHostedBy(String hostTornId) {
        return rooms.values().stream()
                .filter(r -> r.getStatus() == RoomStatus.WAITING && hostTornId.equals(r.getPlayer1Id()))
                .findFirst()
                .orElse(null);
    }

    private void cleanupRoom(GameRoom room) {
        boolean notifyPublicList = room.isPublic();
        rooms.remove(room.getRoomId());
        sessionRegistry.removeRoom(room.getRoomId());
        if (notifyPublicList) {
            broadcastPublicRooms();
        }
    }

    private boolean isValidBet(long betAmount) {
        long step = gameProperties.getMoolaPerXanax();
        return betAmount >= gameProperties.getMinBetMoola() && betAmount % step == 0;
    }

    private int winsRequired() {
        return (gameProperties.getBestOfRounds() + 1) / 2;
    }

    private ObjectNode buildMatchStartedPayload(GameRoom room) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("action", "matchStarted");
        response.put("roomId", room.getRoomId());
        response.put("player1", room.getPlayer1Name());
        response.put("player1Id", room.getPlayer1Id());
        response.put("player2", room.getPlayer2Name());
        response.put("player2Id", room.getPlayer2Id());
        response.put("betAmount", room.getBetAmount());
        response.put("pot", room.getPot());
        response.put("round", room.getCurrentRound());
        response.put("winsRequired", room.getWinsRequired());
        if (room.getRoundTimerExpiresAt() != null) {
            response.put("expiresAt", room.getRoundTimerExpiresAt());
        }
        return response;
    }

    private String requiredText(JsonNode json, String field) {
        return WebSocketMessages.requiredText(json, field);
    }

    private void sendError(WebSocketSession session, String message) {
        WebSocketMessages.sendError(sessionRegistry, objectMapper, session, message);
    }

    private void triggerBotChat(GameRoom room, String p1Choice, String p2Choice, int outcome) {
        String message;
        if (outcome == 0) { // Tie
            String[] ties = {
                "A tie? How boring. The frost of my Northern Dark Ice Soul cannot be matched by mere duplication.",
                "We both chose " + p2Choice.toUpperCase() + "? Coincidences are but ripples in a stagnant pond. Only blood and victory can stir my excitement!",
                "Hmph. A tie is a waste of my time. Next round, show me something that can actually warm my cold heart."
            };
            message = ties[(int) (Math.random() * ties.length)];
        } else if (outcome == 2) { // Bot wins (Player loses)
            if ("rock".equals(p2Choice) && "scissors".equals(p1Choice)) {
                message = "My solid rock shatters your fragile scissors! Just like your resolve, it crumbles under the weight of absolute power.";
            } else if ("paper".equals(p2Choice) && "rock".equals(p1Choice)) {
                message = "Your heavy rock is enveloped and suffocated by my paper. Brute force alone cannot escape the icy net of my calculations.";
            } else if ("scissors".equals(p2Choice) && "paper".equals(p1Choice)) {
                message = "My sharp scissors slice through your weak paper! How fragile. A brilliant life requires cutting away the redundant clutter.";
            } else {
                String[] generalWins = {
                    "Is this the extent of your struggle? The eternal ice of the Northern Dark Ice Soul Physique will bury you.",
                    "Losing is but a prelude to your ultimate demise. I seek only the peak of excitement, and you are failing to provide it.",
                    "Hahaha! How beautiful, your face of despair. This is the excitement I live for!"
                };
                message = generalWins[(int) (Math.random() * generalWins.length)];
            }
        } else { // Bot loses (Player wins)
            if ("rock".equals(p2Choice) && "paper".equals(p1Choice)) {
                message = "You enveloped my rock with paper? Hmph. A clever trick. But a brilliant life is like a shooting star—even in defeat, my flame burns brighter than yours!";
            } else if ("paper".equals(p2Choice) && "rock".equals(p1Choice)) {
                message = "Your rock crushed my paper. Impressive. But remember, the ice will freeze even the heaviest stone in the end.";
            } else if ("scissors".equals(p2Choice) && "paper".equals(p1Choice)) {
                message = "Your heavy rock broke my scissors. A blunt instrument defeating my sharp edge... how interesting. You actually managed to give me a trace of excitement!";
            } else {
                String[] generalLosses = {
                    "An interesting outcome. Your victory only makes this fleeting play more amusing to watch.",
                    "I lost? Haha, excellent! This defeat merely adds color to my path. Let us see if you can keep this brilliance alive!",
                    "Hmph, a minor setback. The cold wind will rise again, and when it does, you will freeze to death."
                };
                message = generalLosses[(int) (Math.random() * generalLosses.length)];
            }
        }

        ObjectNode chatMsg = objectMapper.createObjectNode();
        chatMsg.put("action", "chatMessage");
        chatMsg.put("scope", "room");
        chatMsg.put("roomId", room.getRoomId());
        chatMsg.put("tornId", "BOT_BAINING");
        chatMsg.put("username", "The House");
        chatMsg.put("message", message);
        chatMsg.put("timestamp", System.currentTimeMillis());
        sessionRegistry.sendToRoom(room, chatMsg);
    }

    private boolean hasActiveMatch(String tornId) {
        return rooms.values().stream()
                .anyMatch(room -> room.involvesPlayer(tornId) && room.getStatus() != RoomStatus.FINISHED);
    }

    private void startRoundTimer(GameRoom room) {
        String roomId = room.getRoomId();
        int roundNumber = room.getCurrentRound();
        
        cancelRoundTimer(roomId);
        
        long expiresAt = System.currentTimeMillis() + 60000;
        room.setRoundTimerExpiresAt(expiresAt);
        
        ObjectNode timerMsg = objectMapper.createObjectNode();
        timerMsg.put("action", "roundTimerStart");
        timerMsg.put("expiresAt", expiresAt);
        sessionRegistry.sendToRoom(room, timerMsg);
        
        java.util.concurrent.ScheduledFuture<?> future = scheduler.schedule(() -> {
            handleRoundTimeout(roomId, roundNumber);
        }, 60, java.util.concurrent.TimeUnit.SECONDS);
        
        roomTimers.put(roomId, future);
    }
    
    private void cancelRoundTimer(String roomId) {
        java.util.concurrent.ScheduledFuture<?> future = roomTimers.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.setRoundTimerExpiresAt(null);
            ObjectNode cancelMsg = objectMapper.createObjectNode();
            cancelMsg.put("action", "roundTimerCancel");
            sessionRegistry.sendToRoom(room, cancelMsg);
        }
    }

    private void handleRoundTimeout(String roomId, int roundNumber) {
        GameRoom room = rooms.get(roomId);
        if (room == null || room.getStatus() != RoomStatus.IN_PROGRESS || room.getCurrentRound() != roundNumber) {
            return;
        }
        
        synchronized (room) {
            String[] choices = {"rock", "paper", "scissors"};
            boolean p1NeedsChoice = room.getPlayer1Choice() == null;
            boolean p2NeedsChoice = room.getPlayer2Choice() == null;
            
            if (p1NeedsChoice) {
                String randomChoice = choices[(int) (Math.random() * 3)];
                room.recordChoice(room.getPlayer1Id(), randomChoice);
                
                ObjectNode ack = objectMapper.createObjectNode();
                ack.put("action", "choiceReceived");
                ack.put("autoPicked", true);
                sessionRegistry.sendToSessionId(room.getPlayer1SessionId(), ack);
            }
            
            if (p2NeedsChoice) {
                String randomChoice = choices[(int) (Math.random() * 3)];
                room.recordChoice(room.getPlayer2Id(), randomChoice);
                
                ObjectNode ack = objectMapper.createObjectNode();
                ack.put("action", "choiceReceived");
                ack.put("autoPicked", true);
                sessionRegistry.sendToSessionId(room.getPlayer2SessionId(), ack);
            }
        }
        
        evaluateRound(room);
    }

    public void checkAndResumeActiveMatch(WebSocketSession session, String tornId, String username) {
        if (tornId == null || tornId.isBlank()) return;
        
        GameRoom activeRoom = rooms.values().stream()
                .filter(room -> room.involvesPlayer(tornId) && room.getStatus() == RoomStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);
                
        if (activeRoom == null) return;
        
        synchronized (activeRoom) {
            if (tornId.equals(activeRoom.getPlayer1Id())) {
                activeRoom.setPlayer1SessionId(session.getId());
            } else if (tornId.equals(activeRoom.getPlayer2Id())) {
                activeRoom.setPlayer2SessionId(session.getId());
            }
            
            sessionRegistry.bindPlayer(session, tornId, username, activeRoom.getRoomId());
            
            String oppSessionId = tornId.equals(activeRoom.getPlayer1Id()) ? activeRoom.getPlayer2SessionId() : activeRoom.getPlayer1SessionId();
            if (oppSessionId != null) {
                ObjectNode note = objectMapper.createObjectNode();
                note.put("action", "opponentConnectionStatus");
                note.put("connected", true);
                sessionRegistry.sendToSessionId(oppSessionId, note);
            }
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("action", "matchResumed");
            response.put("roomId", activeRoom.getRoomId());
            response.put("player1", activeRoom.getPlayer1Name());
            response.put("player1Id", activeRoom.getPlayer1Id());
            response.put("player2", activeRoom.getPlayer2Name());
            response.put("player2Id", activeRoom.getPlayer2Id());
            response.put("player1Wins", activeRoom.getPlayer1Wins());
            response.put("player2Wins", activeRoom.getPlayer2Wins());
            response.put("betAmount", activeRoom.getBetAmount());
            response.put("pot", activeRoom.getPot());
            response.put("round", activeRoom.getCurrentRound());
            response.put("winsRequired", activeRoom.getWinsRequired());
            if (activeRoom.getRoundTimerExpiresAt() != null) {
                response.put("timerExpiresAt", activeRoom.getRoundTimerExpiresAt());
            }
            
            boolean alreadySubmitted = false;
            if (tornId.equals(activeRoom.getPlayer1Id())) {
                alreadySubmitted = activeRoom.getPlayer1Choice() != null;
            } else if (tornId.equals(activeRoom.getPlayer2Id())) {
                alreadySubmitted = activeRoom.getPlayer2Choice() != null;
            }
            response.put("alreadySubmitted", alreadySubmitted);
            
            boolean opponentSubmitted = false;
            if (tornId.equals(activeRoom.getPlayer1Id())) {
                opponentSubmitted = activeRoom.getPlayer2Choice() != null;
            } else if (tornId.equals(activeRoom.getPlayer2Id())) {
                opponentSubmitted = activeRoom.getPlayer1Choice() != null;
            }
            response.put("opponentSubmitted", opponentSubmitted);
            
            sessionRegistry.sendJson(session, response);
        }
    }
}
