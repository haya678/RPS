package com.xanwar.rps.dto;

import com.xanwar.rps.model.User;

public record UserDto(
        String tornId,
        String username,
        long siteBalance,
        String profileImageUrl,
        long totalMoolaBetted,
        long totalMoolaWon,
        long totalMoolaLost,
        int totalMatchesPlayed,
        int totalMatchesWon
) {

    public double winRate() {
        return totalMatchesPlayed == 0 ? 0.0
                : (totalMatchesWon * 100.0) / totalMatchesPlayed;
    }

    public long netProfitLoss() {
        return totalMoolaWon - totalMoolaLost;
    }

    public static UserDto from(User user) {
        return new UserDto(
                user.getTornId(),
                user.getUsername(),
                user.safeBalance(),
                user.getProfileImageUrl(),
                user.safeMoolaBetted(),
                user.safeMoolaWon(),
                user.safeMoolaLost(),
                user.safeMatchesPlayed(),
                user.safeMatchesWon()
        );
    }
}
