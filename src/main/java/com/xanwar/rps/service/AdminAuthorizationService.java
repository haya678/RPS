package com.xanwar.rps.service;

import com.xanwar.rps.config.AdminProperties;
import com.xanwar.rps.config.TornApiProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AdminAuthorizationService {

    private final AdminProperties adminProperties;
    private final TornApiProperties tornApiProperties;

    public AdminAuthorizationService(AdminProperties adminProperties, TornApiProperties tornApiProperties) {
        this.adminProperties = adminProperties;
        this.tornApiProperties = tornApiProperties;
    }

    public boolean isAuthorized(String provided) {
        if (provided == null || provided.isBlank()) {
            return false;
        }
        // Reject the well-known placeholder to prevent accidental admin access
        if (!adminProperties.isDefaultKey()
                && constantTimeEquals(adminProperties.getSecretKey(), provided)) {
            return true;
        }
        return constantTimeEquals(tornApiProperties.getMyKey(), provided);
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
