package com.xanwar.rps.game;

public record PotSettlement(long pot, long rake, long winnerPayout) {

    public static PotSettlement fromPot(long pot, int rakePercent) {
        long rake = (pot * rakePercent) / 100;
        long winnerPayout = pot - rake;
        return new PotSettlement(pot, rake, winnerPayout);
    }
}
