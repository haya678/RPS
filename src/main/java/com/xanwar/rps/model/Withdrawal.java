package com.xanwar.rps.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Manual Xanax payout ticket. Moola is deducted when status is {@code pending};
 * admin marks {@code completed} after sending Xanax in Torn.
 */
@Entity
@Table(
        name = "withdrawals",
        indexes = {
                @Index(name = "idx_withdrawals_status", columnList = "status"),
                @Index(name = "idx_withdrawals_torn_id", columnList = "torn_id")
        }
)
public class Withdrawal {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "torn_id", nullable = false, length = 32)
    private String tornId;

    @Column(length = 64)
    private String username;

    @Column(name = "moola_amount", nullable = false)
    private Long moolaAmount;

    /** Whole Xanax to send in-game (moola_amount / 4). */
    @Column(name = "xanax_amount", nullable = false)
    private Integer xanaxAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false, length = 32)
    private String status = STATUS_PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Withdrawal(String tornId, String username, Long moolaAmount, Integer xanaxAmount) {
        this.tornId = tornId;
        this.username = username;
        this.moolaAmount = moolaAmount;
        this.xanaxAmount = xanaxAmount;
    }

    public Withdrawal() {}

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

    public Long getMoolaAmount() {
        return moolaAmount;
    }

    public void setMoolaAmount(Long moolaAmount) {
        this.moolaAmount = moolaAmount;
    }

    public Integer getXanaxAmount() {
        return xanaxAmount;
    }

    public void setXanaxAmount(Integer xanaxAmount) {
        this.xanaxAmount = xanaxAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = STATUS_PENDING;
        }
    }
}
