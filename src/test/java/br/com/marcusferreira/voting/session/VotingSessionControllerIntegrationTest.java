package br.com.marcusferreira.voting.session;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class VotingSessionControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void openSessionReturns201WithTheVotingScreen() {
        Long agendaId = createAgenda("Pauta com sessão");

        ResponseEntity<String> response = openSession(agendaId, 120);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(JsonPath.<String>read(response.getBody(), "$.tipo")).isEqualTo("SELECAO");
    }

    @Test
    void openSessionWithoutBodyUsesDefaultDuration() {
        Long agendaId = createAgenda("Pauta com duração padrão");

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/sessoes", json(""), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String result = restTemplate.getForObject("/api/v1/pautas/" + agendaId + "/resultado", String.class);
        assertThat(result).contains("Sessão em andamento até");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-100", "86401", "10000000000000", "9223372036854775807"})
    void openSessionWithOutOfRangeDurationReturns400AndKeepsAgendaUsable(String duration) {
        Long agendaId = createAgenda("Pauta com duração inválida " + duration);

        ResponseEntity<String> rejected = restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            json("{\"duracaoSegundos\":" + duration + "}"), String.class);
        ResponseEntity<String> retry = openSession(agendaId, 60);

        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rejected.getBody()).contains("\"duracaoSegundos\"");
        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void openingSecondSessionForSameAgendaReturns409() {
        Long agendaId = createAgenda("Pauta duplicada");
        openSession(agendaId, 120);

        assertThat(openSession(agendaId, 120).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void openingNewSessionAfterPreviousSessionClosedReturns409() {
        Long agendaId = createAgenda("Pauta com sessão encerrada");
        assertThat(openSession(agendaId, 1).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        clock.advance(Duration.ofSeconds(2));

        assertThat(openSession(agendaId, 120).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
