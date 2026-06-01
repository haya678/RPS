package com.xanwar.rps.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.xanwar.rps.model.Withdrawal;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WithdrawalDto(
        Long id,
        String tornId,
        String username,
        long moolaAmount,
        int xanaxAmount,
        String status,
        Instant createdAt,
        Instant completedAt
) {

    public static WithdrawalDto from(Withdrawal w) {
        return new WithdrawalDto(
                w.getId(),
                w.getTornId(),
                w.getUsername(),
                w.getMoolaAmount(),
                w.getXanaxAmount(),
                w.getStatus(),
                w.getCreatedAt(),
                w.getCompletedAt()
        );
    }
}
