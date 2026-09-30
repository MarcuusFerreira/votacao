package br.com.marcusferreira.voting.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VotingSessionTest {

    private static final Instant OPENED_AT = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void closesAtIsOpeningTimePlusDuration() {
        VotingSession session = new VotingSession(1L, OPENED_AT, Duration.ofSeconds(60));

        assertThat(session.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(session.getClosesAt()).isEqualTo(OPENED_AT.plusSeconds(60));
    }

    @Test
    void isOpenUntilJustBeforeClosingTime() {
        VotingSession session = new VotingSession(1L, OPENED_AT, Duration.ofSeconds(60));

        assertThat(session.isOpen(OPENED_AT.plusSeconds(59))).isTrue();
        assertThat(session.isOpen(OPENED_AT.plusSeconds(60))).isFalse();
    }
}
