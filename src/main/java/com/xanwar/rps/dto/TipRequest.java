package com.xanwar.rps.dto;

public record TipRequest(
        String fromTornId,
        String toTornId,
        long amount
) {}
