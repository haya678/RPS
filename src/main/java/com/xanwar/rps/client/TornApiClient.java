package com.xanwar.rps.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xanwar.rps.config.TornApiProperties;
import com.xanwar.rps.exception.TornApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TornApiClient {

    private static final Logger log = LoggerFactory.getLogger(TornApiClient.class);

    private final RestTemplate restTemplate;
    private final TornApiProperties tornApi;
    private final ObjectMapper objectMapper;

    public TornApiClient(RestTemplate restTemplate, TornApiProperties tornApi, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.tornApi = tornApi;
        this.objectMapper = objectMapper;
    }

    /** Validates a player's API key and returns basic profile JSON. */
    public JsonNode fetchUserBasic(String playerApiKey) {
        String url = tornApi.getBaseUrl() + "/user/?selections=basic&key=" + playerApiKey;
        return getJson(url);
    }

    public JsonNode fetchUserProfile(String playerApiKey) {
        String url = tornApi.getBaseUrl() + "/user/?selections=profile,basic&key=" + playerApiKey;
        return getJson(url);
    }

    /** House account profile (recipient for deposits). */
    public JsonNode fetchHouseBasic() {
        return fetchUserBasic(tornApi.getMyKey());
    }

    /**
     * Fetches house events + log (deposit verification).
     * Does NOT use the Torn {@code from} parameter because it behaves
     * inconsistently across {@code events} and {@code log} selections.
     * Instead we fetch the latest batch and filter by timestamp client-side.
     */
    public JsonNode fetchHouseActivity() {
        String url = tornApi.getBaseUrl()
                + "/user/?selections=events,log"
                + "&key=" + tornApi.getMyKey()
                + "&comment=" + System.currentTimeMillis();
        log.debug("Fetching house activity from Torn API");
        return getJson(url);
    }

    private JsonNode getJson(String url) {
        try {
            String body = restTemplate.getForObject(url, String.class);
            log.debug("Torn API raw response (first 500 chars): {}",
                    body != null ? body.substring(0, Math.min(500, body.length())) : "null");
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error")) {
                String code = root.path("error").path("code").asText("?");
                String msg = root.path("error").path("error").asText("Unknown Torn API error");
                log.error("Torn API error (code {}): {}", code, msg);
                throw new TornApiException("Torn API error " + code + ": " + msg);
            }
            return root;
        } catch (TornApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Torn API: {}", e.getMessage(), e);
            throw new TornApiException("Failed to call Torn API: " + e.getMessage(), e);
        }
    }
}
