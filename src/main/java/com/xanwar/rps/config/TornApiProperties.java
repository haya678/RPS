package com.xanwar.rps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "torn.api")
public class TornApiProperties {

    private String baseUrl = "https://api.torn.com";
    private String myKey = "";
    /** Path only, e.g. /user/?selections=events */
    private String eventsPath = "/user/?selections=events";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getMyKey() {
        return myKey;
    }

    public void setMyKey(String myKey) {
        this.myKey = myKey;
    }

    public String getEventsPath() {
        return eventsPath;
    }

    public void setEventsPath(String eventsPath) {
        this.eventsPath = eventsPath;
    }
}
