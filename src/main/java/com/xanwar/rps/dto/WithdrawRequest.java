package com.xanwar.rps.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WithdrawRequest(
        String tornId,
        @NotNull @Min(1) Long moolaAmount
) {}
