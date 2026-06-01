package com.xanwar.rps.model;

import java.util.Objects;

public class GameRoom {

    private final String roomId;
    private final String player1Id;
    private final String player1Name;
    private final long betAmount;
    private final int winsRequired;

    private String player2Id;
    private String player2Name;
    private String player1SessionId;
    private String player2SessionId;

    private long pot;
    private int player1Wins;
    private int player2Wins;
    private int currentRound = 1;
    private String player1Choice;
    private String player2Choice;
    private RoomStatus status = RoomStatus.WAITING;
    private final RoomVisibility visibility;
    private final long createdAtEpochMs;

    public GameRoom(
            String roomId,
            String player1Id,
            String player1Name,
            long betAmount,
            int winsRequired,
            String player1SessionId,
            RoomVisibility visibility
    ) {
        this.roomId = roomId;
        this.player1Id = player1Id;
        this.player1Name = player1Name;
        this.betAmount = betAmount;
        this.winsRequired = winsRequired;
        this.player1SessionId = player1SessionId;
        this.visibility = visibility != null ? visibility : RoomVisibility.PRIVATE;
        this.pot = betAmount;
        this.createdAtEpochMs = System.currentTimeMillis();
    }

    public synchronized boolean tryStartMatch(String player2Id, String player2Name, String player2SessionId) {
        if (status != RoomStatus.WAITING || this.player2Id != null) {
            return false;
        }
        this.player2Id = player2Id;
        this.player2Name = player2Name;
        this.player2SessionId = player2SessionId;
        this.pot = betAmount * 2;
        this.status = RoomStatus.IN_PROGRESS;
        return true;
    }

    public synchronized boolean recordChoice(String tornId, String choice) {
        if (status != RoomStatus.IN_PROGRESS) {
            return false;
        }
        if (tornId.equals(player1Id)) {
            if (player1Choice != null) {
                return false;
            }
            player1Choice = choice;
            return true;
        }
        if (tornId.equals(player2Id)) {
            if (player2Choice != null) {
                return false;
            }
            player2Choice = choice;
            return true;
        }
        return false;
    }

    public synchronized boolean bothChoicesSubmitted() {
        return player1Choice != null && player2Choice != null;
    }

    public synchronized void clearRoundChoices() {
        player1Choice = null;
        player2Choice = null;
    }

    public synchronized boolean isMatchOver() {
        return player1Wins >= winsRequired || player2Wins >= winsRequired;
    }

    public synchronized String opponentId(String tornId) {
        if (tornId.equals(player1Id)) {
            return player2Id;
        }
        if (Objects.equals(tornId, player2Id)) {
            return player1Id;
        }
        return null;
    }

    public synchronized boolean involvesPlayer(String tornId) {
        return tornId.equals(player1Id) || tornId.equals(player2Id);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPlayer1Id() {
        return player1Id;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public long getPot() {
        return pot;
    }

    public int getPlayer1Wins() {
        return player1Wins;
    }

    public void setPlayer1Wins(int player1Wins) {
        this.player1Wins = player1Wins;
    }

    public int getPlayer2Wins() {
        return player2Wins;
    }

    public void setPlayer2Wins(int player2Wins) {
        this.player2Wins = player2Wins;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public String getPlayer1Choice() {
        return player1Choice;
    }

    public String getPlayer2Choice() {
        return player2Choice;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getPlayer1SessionId() {
        return player1SessionId;
    }

    public void setPlayer1SessionId(String player1SessionId) {
        this.player1SessionId = player1SessionId;
    }

    public String getPlayer2SessionId() {
        return player2SessionId;
    }

    public int getWinsRequired() {
        return winsRequired;
    }

    public RoomVisibility getVisibility() {
        return visibility;
    }

    public boolean isPublic() {
        return visibility == RoomVisibility.PUBLIC;
    }

    public long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }
}
