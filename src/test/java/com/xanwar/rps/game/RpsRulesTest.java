package com.xanwar.rps.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RpsRulesTest {

    @ParameterizedTest
    @CsvSource({
            "rock,     scissors, 1",
            "scissors, paper,    1",
            "paper,    rock,     1",
            "scissors, rock,     2",
            "paper,    scissors, 2",
            "rock,     paper,    2",
            "rock,     rock,     0",
            "paper,    paper,    0",
            "scissors, scissors, 0"
    })
    void roundWinnerCoversAllOutcomes(String p1, String p2, int expected) {
        assertThat(RpsRules.roundWinner(p1, p2)).isEqualTo(expected);
    }

    @Test
    void isValidChoiceAcceptsRockPaperScissors() {
        assertThat(RpsRules.isValidChoice("rock")).isTrue();
        assertThat(RpsRules.isValidChoice("paper")).isTrue();
        assertThat(RpsRules.isValidChoice("scissors")).isTrue();
    }

    @Test
    void isValidChoiceRejectsInvalidInputs() {
        assertThat(RpsRules.isValidChoice("lizard")).isFalse();
        assertThat(RpsRules.isValidChoice("")).isFalse();
        assertThat(RpsRules.isValidChoice(null)).isFalse();
        assertThat(RpsRules.isValidChoice("ROCK")).isFalse();
    }

    @Test
    void validChoicesSetContainsExactlyThreeEntries() {
        assertThat(RpsRules.VALID_CHOICES).containsExactlyInAnyOrder("rock", "paper", "scissors");
    }
}
