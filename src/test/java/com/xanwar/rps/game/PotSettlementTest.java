package com.xanwar.rps.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PotSettlementTest {

    @Test
    void standardRakeCalculation() {
        PotSettlement settlement = PotSettlement.fromPot(100, 3);

        assertThat(settlement.pot()).isEqualTo(100);
        assertThat(settlement.rake()).isEqualTo(3);
        assertThat(settlement.winnerPayout()).isEqualTo(97);
    }

    @Test
    void zeroRakeGivesFullPotToWinner() {
        PotSettlement settlement = PotSettlement.fromPot(200, 0);

        assertThat(settlement.rake()).isEqualTo(0);
        assertThat(settlement.winnerPayout()).isEqualTo(200);
    }

    @Test
    void hundredPercentRakeGivesNothingToWinner() {
        PotSettlement settlement = PotSettlement.fromPot(50, 100);

        assertThat(settlement.rake()).isEqualTo(50);
        assertThat(settlement.winnerPayout()).isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource({
            "8,   3, 0, 8",
            "1000, 5, 50, 950",
            "7,   10, 0, 7"
    })
    void rakeAndPayoutSumToPot(long pot, int rakePercent, long expectedRake, long expectedPayout) {
        PotSettlement settlement = PotSettlement.fromPot(pot, rakePercent);

        assertThat(settlement.rake()).isEqualTo(expectedRake);
        assertThat(settlement.winnerPayout()).isEqualTo(expectedPayout);
        assertThat(settlement.rake() + settlement.winnerPayout()).isEqualTo(pot);
    }

    @Test
    void integerTruncationForSmallPots() {
        // 7 * 3 / 100 = 0 (integer division)
        PotSettlement settlement = PotSettlement.fromPot(7, 3);

        assertThat(settlement.rake()).isEqualTo(0);
        assertThat(settlement.winnerPayout()).isEqualTo(7);
    }
}
