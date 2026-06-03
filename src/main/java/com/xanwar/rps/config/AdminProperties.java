package com.xanwar.rps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    private static final String INSECURE_DEFAULT = "change-this-to-a-secure-admin-key";

    private String secretKey = INSECURE_DEFAULT;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /** Returns true if the admin secret is still the placeholder default. */
    public boolean isDefaultKey() {
        return INSECURE_DEFAULT.equals(secretKey);
    }
}
