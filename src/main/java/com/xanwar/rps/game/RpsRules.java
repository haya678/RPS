package com.xanwar.rps.game;

import java.util.Set;

public final class RpsRules {

    public static final Set<String> VALID_CHOICES = Set.of("rock", "paper", "scissors");

    private RpsRules() {}

    /**
     * @return 1 if player1 wins, 2 if player2 wins, 0 for tie
     */
    public static int roundWinner(String player1Choice, String player2Choice) {
        if (player1Choice.equals(player2Choice)) {
            return 0;
        }
        if (beats(player1Choice, player2Choice)) {
            return 1;
        }
        return 2;
    }

    private static boolean beats(String a, String b) {
        return (a.equals("rock") && b.equals("scissors"))
                || (a.equals("scissors") && b.equals("paper"))
                || (a.equals("paper") && b.equals("rock"));
    }

    public static boolean isValidChoice(String choice) {
        return choice != null && VALID_CHOICES.contains(choice);
    }
}
