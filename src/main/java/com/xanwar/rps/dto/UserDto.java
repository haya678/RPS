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

    public static UserDto from(User user) {
        long siteBalance = user.getSiteBalance() == null ? 0L : user.getSiteBalance();
        long totalBetted = user.getTotalMoolaBetted() == null ? 0L : user.getTotalMoolaBetted();
        long totalWon = user.getTotalMoolaWon() == null ? 0L : user.getTotalMoolaWon();
        long totalLost = user.getTotalMoolaLost() == null ? 0L : user.getTotalMoolaLost();
        int matchesPlayed = user.getTotalMatchesPlayed() == null ? 0 : user.getTotalMatchesPlayed();
        int matchesWon = user.getTotalMatchesWon() == null ? 0 : user.getTotalMatchesWon();
        return new UserDto(
                user.getTornId(),
                user.getUsername(),
                siteBalance,
                user.getProfileImageUrl(),
                totalBetted,
                totalWon,
                totalLost,
                matchesPlayed,
                matchesWon
        );
    }
}
