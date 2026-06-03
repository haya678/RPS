package com.xanwar.rps.game;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TornXanaxDepositParserTest {

    @Test
    void parsesReceivedEventWithBracketedSenderId() {
        Optional<TornXanaxDepositParser.ParsedDeposit> parsed = TornXanaxDepositParser.parse(
                "You received 3x Xanax from Player [12345] with the message: RPS.",
                "RPS"
        );

        assertThat(parsed).isPresent();
        assertThat(parsed.get().xanaxAmount()).isEqualTo(3);
        assertThat(parsed.get().senderTornId()).isEqualTo("12345");
    }

    @Test
    void preservesSenderIdFromTornProfileLink() {
        Optional<TornXanaxDepositParser.ParsedDeposit> parsed = TornXanaxDepositParser.parse(
                "<a href=\"/profiles.php?XID=222333\">Player</a> sent you 2 Xanax with the message: RPS",
                "RPS"
        );

        assertThat(parsed).isPresent();
        assertThat(parsed.get().xanaxAmount()).isEqualTo(2);
        assertThat(parsed.get().senderTornId()).isEqualTo("222333");
    }

    @Test
    void acceptsFallbackSenderIdWhenTextOmitsId() {
        Optional<TornXanaxDepositParser.ParsedDeposit> parsed = TornXanaxDepositParser.parse(
                "Player sent you 1x Xanax with message: \"rps\"",
                "RPS",
                "98765"
        );

        assertThat(parsed).isPresent();
        assertThat(parsed.get().xanaxAmount()).isEqualTo(1);
        assertThat(parsed.get().senderTornId()).isEqualTo("98765");
    }

    @Test
    void rejectsWrongDepositMessage() {
        Optional<TornXanaxDepositParser.ParsedDeposit> parsed = TornXanaxDepositParser.parse(
                "You received 1x Xanax from Player [12345] with the message: wrong",
                "RPS"
        );

        assertThat(parsed).isEmpty();
    }
}
