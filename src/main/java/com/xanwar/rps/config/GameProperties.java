package com.xanwar.rps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game")
public class GameProperties {

    private int moolaPerXanax = 4;
    private int houseRakePercent = 3;
    private long minBetMoola = 4;
    private int bestOfRounds = 3;
    private long withdrawalMoolaStep = 4;
    private Websocket websocket = new Websocket();

    public static class Websocket {
        private String path = "/ws/game";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public int getMoolaPerXanax() {
        return moolaPerXanax;
    }

    public void setMoolaPerXanax(int moolaPerXanax) {
        this.moolaPerXanax = moolaPerXanax;
    }

    public int getHouseRakePercent() {
        return houseRakePercent;
    }

    public void setHouseRakePercent(int houseRakePercent) {
        this.houseRakePercent = houseRakePercent;
    }

    public long getMinBetMoola() {
        return minBetMoola;
    }

    public void setMinBetMoola(long minBetMoola) {
        this.minBetMoola = minBetMoola;
    }

    public int getBestOfRounds() {
        return bestOfRounds;
    }

    public void setBestOfRounds(int bestOfRounds) {
        this.bestOfRounds = bestOfRounds;
    }

    public long getWithdrawalMoolaStep() {
        return withdrawalMoolaStep;
    }

    public void setWithdrawalMoolaStep(long withdrawalMoolaStep) {
        this.withdrawalMoolaStep = withdrawalMoolaStep;
    }

    public Websocket getWebsocket() {
        return websocket;
    }

    public void setWebsocket(Websocket websocket) {
        this.websocket = websocket;
    }
}
