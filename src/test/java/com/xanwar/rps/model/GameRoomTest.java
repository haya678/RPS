package com.xanwar.rps.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameRoomTest {

    private GameRoom room;

    @BeforeEach
    void setUp() {
        room = new GameRoom("room1", "player1", "Alice", 100L, 2, "session1", RoomVisibility.PUBLIC);
    }

    @Test
    void initialStateIsWaitingWithPlayer1Bet() {
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.getPot()).isEqualTo(100L);
        assertThat(room.getPlayer1Id()).isEqualTo("player1");
        assertThat(room.getPlayer1Name()).isEqualTo("Alice");
        assertThat(room.getCurrentRound()).isEqualTo(1);
        assertThat(room.getPlayer1Wins()).isEqualTo(0);
        assertThat(room.getPlayer2Wins()).isEqualTo(0);
    }

    @Test
    void tryStartMatchSetsPlayer2AndDoublesPot() {
        boolean started = room.tryStartMatch("player2", "Bob", "session2");

        assertThat(started).isTrue();
        assertThat(room.getPlayer2Id()).isEqualTo("player2");
        assertThat(room.getPlayer2Name()).isEqualTo("Bob");
        assertThat(room.getPot()).isEqualTo(200L);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
    }

    @Test
    void tryStartMatchFailsIfAlreadyStarted() {
        room.tryStartMatch("player2", "Bob", "session2");
        boolean secondAttempt = room.tryStartMatch("player3", "Charlie", "session3");

        assertThat(secondAttempt).isFalse();
        assertThat(room.getPlayer2Id()).isEqualTo("player2");
    }

    @Test
    void recordChoiceOnlyAllowedDuringInProgress() {
        // Room is still WAITING
        boolean result = room.recordChoice("player1", "rock");
        assertThat(result).isFalse();
    }

    @Test
    void recordChoiceAcceptsValidPlayerChoices() {
        room.tryStartMatch("player2", "Bob", "session2");

        assertThat(room.recordChoice("player1", "rock")).isTrue();
        assertThat(room.recordChoice("player2", "paper")).isTrue();
        assertThat(room.bothChoicesSubmitted()).isTrue();
    }

    @Test
    void recordChoiceRejectsDuplicateFromSamePlayer() {
        room.tryStartMatch("player2", "Bob", "session2");

        room.recordChoice("player1", "rock");
        boolean duplicate = room.recordChoice("player1", "paper");

        assertThat(duplicate).isFalse();
    }

    @Test
    void recordChoiceRejectsUnknownPlayer() {
        room.tryStartMatch("player2", "Bob", "session2");

        boolean result = room.recordChoice("stranger", "rock");
        assertThat(result).isFalse();
    }

    @Test
    void clearRoundChoicesResetsSelections() {
        room.tryStartMatch("player2", "Bob", "session2");
        room.recordChoice("player1", "rock");
        room.recordChoice("player2", "scissors");

        room.clearRoundChoices();

        assertThat(room.bothChoicesSubmitted()).isFalse();
        assertThat(room.getPlayer1Choice()).isNull();
        assertThat(room.getPlayer2Choice()).isNull();
    }

    @Test
    void isMatchOverWhenWinsRequiredReached() {
        room.tryStartMatch("player2", "Bob", "session2");

        room.setPlayer1Wins(2);
        assertThat(room.isMatchOver()).isTrue();

        room.setPlayer1Wins(0);
        room.setPlayer2Wins(2);
        assertThat(room.isMatchOver()).isTrue();
    }

    @Test
    void isMatchOverFalseWhenBelowThreshold() {
        room.tryStartMatch("player2", "Bob", "session2");

        room.setPlayer1Wins(1);
        room.setPlayer2Wins(1);
        assertThat(room.isMatchOver()).isFalse();
    }

    @Test
    void opponentIdReturnsCorrectOpponent() {
        room.tryStartMatch("player2", "Bob", "session2");

        assertThat(room.opponentId("player1")).isEqualTo("player2");
        assertThat(room.opponentId("player2")).isEqualTo("player1");
        assertThat(room.opponentId("stranger")).isNull();
    }

    @Test
    void involvesPlayerIdentifiesBothPlayers() {
        room.tryStartMatch("player2", "Bob", "session2");

        assertThat(room.involvesPlayer("player1")).isTrue();
        assertThat(room.involvesPlayer("player2")).isTrue();
        assertThat(room.involvesPlayer("stranger")).isFalse();
    }

    @Test
    void visibilityDefaultsToPrivateWhenNull() {
        GameRoom privateRoom = new GameRoom("r2", "p1", "A", 50, 1, "s1", null);
        assertThat(privateRoom.getVisibility()).isEqualTo(RoomVisibility.PRIVATE);
        assertThat(privateRoom.isPublic()).isFalse();
    }

    @Test
    void publicRoomReportsIsPublicTrue() {
        assertThat(room.isPublic()).isTrue();
    }
}
