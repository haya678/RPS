package com.xanwar.rps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AuthRequest(
        @NotBlank @JsonProperty("api_key") String apiKey,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        @JsonProperty("pin") String pin
) {}
