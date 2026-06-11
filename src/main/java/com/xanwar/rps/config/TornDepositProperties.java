package com.xanwar.rps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "torn.deposit")
public class TornDepositProperties {

    private String requiredMessage = "RPS";
    private int maxAgeHours = 72;
    private String recipientName = "Hannath";
    private String recipientId;

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

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
    }
