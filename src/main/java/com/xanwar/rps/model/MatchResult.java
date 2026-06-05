package com.xanwar.rps.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "match_results")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player1_id", nullable = false)
    private String player1Id;

    @Column(name = "player1_name", nullable = false)
    private String player1Name;

    @Column(name = "player2_id", nullable = false)
    private String player2Id;

    @Column(name = "player2_name", nullable = false)
    private String player2Name;

    @Column(name = "winner_id")
    private String winnerId;

    @Column(name = "pot_amount", nullable = false)
    private Long potAmount;

    @Column(name = "bet_amount", nullable = false)
    private Long betAmount;

    @Column(name = "is_forfeit", nullable = false)
    private boolean isForfeit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public MatchResult() {}

    public MatchResult(String player1Id, String player1Name, String player2Id, String player2Name, String winnerId, Long potAmount, Long betAmount, boolean isForfeit) {
        this.player1Id = player1Id;
        this.player1Name = player1Name;
        this.player2Id = player2Id;
        this.player2Name = player2Name;
        this.winnerId = winnerId;
        this.potAmount = potAmount;
        this.betAmount = betAmount;
        this.isForfeit = isForfeit;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(String player1Id) {
        this.player1Id = player1Id;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public Long getPotAmount() {
        return potAmount;
    }

    public void setPotAmount(Long potAmount) {
        this.potAmount = potAmount;
    }

    public Long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(Long betAmount) {
        this.betAmount = betAmount;
    }

    public boolean isForfeit() {
        return isForfeit;
    }

    public void setForfeit(boolean forfeit) {
        isForfeit = forfeit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
