package com.xanwar.rps.dto;

import java.time.Instant;

public record MatchHistoryDto(
    boolean won,
    long potAmount,
    String opponentName,
    String opponentProfilePic,
    Instant timestamp,
    boolean isForfeit
) {}
