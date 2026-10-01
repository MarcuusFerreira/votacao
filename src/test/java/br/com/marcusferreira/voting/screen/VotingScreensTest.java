package br.com.marcusferreira.voting.screen;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.common.VotingProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class VotingScreensTest {

    private final VotingScreens screens = new VotingScreens(new ScreenUrls("http://localhost:8080"),
        new VotingProperties("http://localhost:8080", ZoneId.of("America/Sao_Paulo"),
            new VotingProperties.Session(Duration.ofSeconds(60)),
            new VotingProperties.Member("http://example.com", false)));

    @Test
    void sessionInProgressShowsClosingTimeInTheDisplayZone() {
        FormScreen screen = screens.resultInProgress(1L, Instant.parse("2026-01-01T15:00:00Z"));

        assertThat(screen.items().getFirst().text()).isEqualTo("Sessão em andamento até 01/01/2026 12:00:00");
    }
}
