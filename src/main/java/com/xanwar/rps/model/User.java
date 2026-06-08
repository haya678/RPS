package com.xanwar.rps.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Registered Torn player and Moola wallet.
 * {@code siteBalance} is the escrowable in-game currency (Moola).
 */
@Entity
@Table(
        name = "users",
        indexes = @Index(name = "idx_users_torn_id", columnList = "torn_id", unique = true)
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Torn numeric player ID (string to preserve leading zeros if any). */
    @Column(name = "torn_id", nullable = false, unique = true, length = 32)
    private String tornId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "site_balance", nullable = false)
    private Long siteBalance = 0L;

    /** Optional: player's own API key if you add profile features later. */
    @Column(name = "api_key_encrypted", length = 512)
    private String apiKeyEncrypted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login", nullable = false)
    private Instant lastLogin = Instant.now();

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "pin_hash", length = 120)
    private String pinHash;

    @Column(name = "remember_token", length = 64)
    private String rememberToken;

    @Column(name = "total_moola_betted", nullable = false)
    private Long totalMoolaBetted = 0L;

    @Column(name = "total_moola_won", nullable = false)
    private Long totalMoolaWon = 0L;

    @Column(name = "total_moola_lost", nullable = false)
    private Long totalMoolaLost = 0L;

    @Column(name = "total_matches_played", nullable = false)
    private Integer totalMatchesPlayed = 0;

    @Column(name = "total_matches_won", nullable = false)
    private Integer totalMatchesWon = 0;

    @Column(nullable = false)
    private Boolean frozen = false;

    @Column(name = "bonus_balance", nullable = false)
    private Long bonusBalance = 0L;

    @Column(name = "wagering_requirement_remaining", nullable = false)
    private Long wageringRequirementRemaining = 0L;

    @Column(name = "wagering_requirement_total", nullable = false)
    private Long wageringRequirementTotal = 0L;

    @Column(name = "pending_deposit_bonus_json", length = 1000)
    private String pendingDepositBonusJson;

    public User(String tornId, String username) {
        this.tornId = tornId;
        this.username = username;
    }

    public User() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTornId() {
        return tornId;
    }

    public void setTornId(String tornId) {
        this.tornId = tornId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getSiteBalance() {
        return siteBalance;
    }

    public void setSiteBalance(Long siteBalance) {
        this.siteBalance = siteBalance;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public void setRememberToken(String rememberToken) {
        this.rememberToken = rememberToken;
    }

    public Long getTotalMoolaBetted() {
        return totalMoolaBetted;
    }

    public void setTotalMoolaBetted(Long totalMoolaBetted) {
        this.totalMoolaBetted = totalMoolaBetted;
    }

    public Long getTotalMoolaWon() {
        return totalMoolaWon;
    }

    public void setTotalMoolaWon(Long totalMoolaWon) {
        this.totalMoolaWon = totalMoolaWon;
    }

    public Long getTotalMoolaLost() {
        return totalMoolaLost;
    }

    public void setTotalMoolaLost(Long totalMoolaLost) {
        this.totalMoolaLost = totalMoolaLost;
    }

    public Integer getTotalMatchesPlayed() {
        return totalMatchesPlayed;
    }

    public void setTotalMatchesPlayed(Integer totalMatchesPlayed) {
        this.totalMatchesPlayed = totalMatchesPlayed;
    }

    public Integer getTotalMatchesWon() {
        return totalMatchesWon;
    }

    public void setTotalMatchesWon(Integer totalMatchesWon) {
        this.totalMatchesWon = totalMatchesWon;
    }

    public Boolean getFrozen() {
        return frozen;
    }

    public void setFrozen(Boolean frozen) {
        this.frozen = frozen;
    }

    public Long getBonusBalance() {
        return bonusBalance;
    }

    public void setBonusBalance(Long bonusBalance) {
        this.bonusBalance = bonusBalance;
    }

    public Long getWageringRequirementRemaining() {
        return wageringRequirementRemaining;
    }

    public void setWageringRequirementRemaining(Long wageringRequirementRemaining) {
        this.wageringRequirementRemaining = wageringRequirementRemaining;
    }

    public Long getWageringRequirementTotal() {
        return wageringRequirementTotal;
    }

    public void setWageringRequirementTotal(Long wageringRequirementTotal) {
        this.wageringRequirementTotal = wageringRequirementTotal;
    }

    public String getPendingDepositBonusJson() {
        return pendingDepositBonusJson;
    }

    public void setPendingDepositBonusJson(String pendingDepositBonusJson) {
        this.pendingDepositBonusJson = pendingDepositBonusJson;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastLogin == null) {
            lastLogin = createdAt;
        }
        if (siteBalance == null) {
            siteBalance = 0L;
        }
        if (totalMoolaBetted == null) {
            totalMoolaBetted = 0L;
        }
        if (totalMoolaWon == null) {
            totalMoolaWon = 0L;
        }
        if (totalMoolaLost == null) {
            totalMoolaLost = 0L;
        }
        if (totalMatchesPlayed == null) {
            totalMatchesPlayed = 0;
        }
        if (totalMatchesWon == null) {
            totalMatchesWon = 0;
        }
        if (bonusBalance == null) {
            bonusBalance = 0L;
        }
        if (wageringRequirementRemaining == null) {
            wageringRequirementRemaining = 0L;
        }
        if (wageringRequirementTotal == null) {
            wageringRequirementTotal = 0L;
        }
    }

    // ── Null-safe convenience getters ──────────────────────────────────

    public long safeBalance()        { return siteBalance        != null ? siteBalance        : 0L; }
    public long safeMoolaBetted()    { return totalMoolaBetted   != null ? totalMoolaBetted   : 0L; }
    public long safeMoolaWon()       { return totalMoolaWon      != null ? totalMoolaWon      : 0L; }
    public long safeMoolaLost()      { return totalMoolaLost     != null ? totalMoolaLost     : 0L; }
    public int  safeMatchesPlayed()  { return totalMatchesPlayed != null ? totalMatchesPlayed : 0;  }
    public int  safeMatchesWon()     { return totalMatchesWon    != null ? totalMatchesWon    : 0;  }
}
