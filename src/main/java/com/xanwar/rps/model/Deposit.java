package com.xanwar.rps.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Verified Xanax deposit from the house Torn event log.
 * {@code eventId} is unique to prevent double-claiming the same Torn event.
 */
@Entity
@Table(
        name = "deposits",
        indexes = {
                @Index(name = "idx_deposits_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_deposits_torn_id", columnList = "torn_id")
        }
)
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Torn API event_id — must be unique across all claims. */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "torn_id", nullable = false, length = 32)
    private String tornId;

    @Column(length = 64)
    private String username;

    @Column(name = "amount_xanax", nullable = false)
    private Integer amountXanax;

    @Column(name = "moola_credited", nullable = false)
    private Long moolaCredited;

    /** When Torn recorded the transfer (used for recency checks). */
    @Column(name = "torn_event_timestamp", nullable = false)
    private Instant tornEventTimestamp;

    /** When this site processed and credited Moola. */
    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt = Instant.now();

    @Column(nullable = false, length = 32)
    private String status = "processed";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Deposit(
            String eventId,
            String tornId,
            String username,
            Integer amountXanax,
            Long moolaCredited,
            Instant tornEventTimestamp
    ) {
        this.eventId = eventId;
        this.tornId = tornId;
        this.username = username;
        this.amountXanax = amountXanax;
        this.moolaCredited = moolaCredited;
        this.tornEventTimestamp = tornEventTimestamp;
    }

    public Deposit() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public Integer getAmountXanax() {
        return amountXanax;
    }

    public void setAmountXanax(Integer amountXanax) {
        this.amountXanax = amountXanax;
    }

    public Long getMoolaCredited() {
        return moolaCredited;
    }

    public void setMoolaCredited(Long moolaCredited) {
        this.moolaCredited = moolaCredited;
    }

    public Instant getTornEventTimestamp() {
        return tornEventTimestamp;
    }

    public void setTornEventTimestamp(Instant tornEventTimestamp) {
        this.tornEventTimestamp = tornEventTimestamp;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        if (claimedAt == null) {
            claimedAt = Instant.now();
        }
        if (status == null) {
            status = "processed";
        }
    }
}
