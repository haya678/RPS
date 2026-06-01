package com.xanwar.rps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "torn.deposit")
public class TornDepositProperties {

    private String requiredMessage = "rps";
    private int maxAgeHours = 72;

    public String getRequiredMessage() {
        return requiredMessage;
    }

    public void setRequiredMessage(String requiredMessage) {
        this.requiredMessage = requiredMessage;
    }

    public int getMaxAgeHours() {
        return maxAgeHours;
    }

    public void setMaxAgeHours(int maxAgeHours) {
        this.maxAgeHours = maxAgeHours;
    }
}
