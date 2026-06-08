package com.xanwar.rps.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pending_deposits", indexes = @Index(name = "idx_pending_deposits_torn_id", columnList = "torn_id", unique = true))
public class PendingDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "torn_id", nullable = false, unique = true, length = 32)
    private String tornId;

    @Column(name = "xanax_amount", nullable = false)
    private Integer xanaxAmount;

    @Column(name = "start_time", nullable = false)
    private Instant startTime = Instant.now();

    @Column(nullable = false, length = 32)
    private String status = "initiating";

    public PendingDeposit() {}

    public PendingDeposit(String tornId, Integer xanaxAmount) {
        this.tornId = tornId;
        this.xanaxAmount = xanaxAmount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTornId() { return tornId; }
    public void setTornId(String tornId) { this.tornId = tornId; }
    public Integer getXanaxAmount() { return xanaxAmount; }
    public void setXanaxAmount(Integer xanaxAmount) { this.xanaxAmount = xanaxAmount; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
