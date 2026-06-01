package com.xanwar.rps.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xanwar.rps.config.TornApiProperties;
import com.xanwar.rps.exception.TornApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TornApiClient {

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

    /** Fetches the house account event log (deposit verification). */
    public JsonNode fetchHouseEvents() {
        return fetchHouseEvents(null);
    }

    /** @param fromEpochSeconds optional; only events at or after this Unix time (Torn {@code from} filter). */
    public JsonNode fetchHouseEvents(Long fromEpochSeconds) {
        String path = tornApi.getEventsPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String separator = path.contains("?") ? "&" : "?";
        StringBuilder url = new StringBuilder(tornApi.getBaseUrl())
                .append(path)
                .append(separator)
                .append("key=")
                .append(tornApi.getMyKey());
        if (fromEpochSeconds != null && fromEpochSeconds > 0) {
            url.append("&from=").append(fromEpochSeconds);
        }
        url.append("&comment=").append(System.currentTimeMillis());
        return getJson(url.toString());
    }

    private JsonNode getJson(String url) {
        try {
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error")) {
                String msg = root.path("error").path("error").asText("Unknown Torn API error");
                throw new TornApiException(msg);
            }
            return root;
        } catch (TornApiException e) {
            throw e;
        } catch (Exception e) {
            throw new TornApiException("Failed to call Torn API: " + e.getMessage(), e);
        }
    }
}
