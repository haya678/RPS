package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.config.TornApiProperties;
import com.xanwar.rps.config.TornDepositProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HouseAccountService {

    private static final Logger log = LoggerFactory.getLogger(HouseAccountService.class);

    private final TornApiClient tornApiClient;
    private final TornApiProperties tornApiProperties;
    private final TornDepositProperties depositProperties;
    private final GameProperties gameProperties;

    private String recipientName;
    private String recipientId;

    public HouseAccountService(
            TornApiClient tornApiClient,
            TornApiProperties tornApiProperties,
            TornDepositProperties depositProperties,
            GameProperties gameProperties
    ) {
        this.tornApiClient = tornApiClient;
        this.tornApiProperties = tornApiProperties;
        this.depositProperties = depositProperties;
        this.gameProperties = gameProperties;
        this.recipientName = depositProperties.getRecipientName();
        this.recipientId = depositProperties.getRecipientId();
    }

    @PostConstruct
    void loadHouseProfile() {
        String key = tornApiProperties.getMyKey();
        if (key == null || key.isBlank() || key.contains("YOUR_PERSONAL")) {
            log.warn("House Torn API key not configured — deposit verification disabled until torn.api.my-key is set");
            return;
        }
        try {
            JsonNode data = tornApiClient.fetchHouseBasic();
            String apiName = data.path("name").asText("");
            int apiId = data.path("player_id").asInt(0);
            if (!apiName.isBlank()) {
                recipientName = apiName;
            }
            if (apiId > 0) {
                recipientId = String.valueOf(apiId);
            }
            log.info("Deposit recipient loaded: {} [{}]", recipientName, recipientId);
        } catch (Exception e) {
            log.error("Could not load house account from Torn API: {}", e.getMessage());
        }
    }

    public Map<String, Object> depositInstructions() {
        Map<String, Object> info = new HashMap<>();
        info.put("recipientName", recipientName);
        info.put("recipientId", recipientId);
        info.put("requiredMessage", depositProperties.getRequiredMessage());
        info.put("moolaPerXanax", gameProperties.getMoolaPerXanax());
        info.put("maxAgeHours", depositProperties.getMaxAgeHours());
        return info;
    }

    public String getRecipientId() {
        return recipientId;
    }
}
