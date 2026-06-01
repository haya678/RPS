package com.xanwar.rps.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminKeyRequest(@NotBlank String adminKey) {}
