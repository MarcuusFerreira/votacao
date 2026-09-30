package br.com.marcusferreira.voting.session;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import br.com.marcusferreira.voting.session.dto.OpenSessionRequest;
import br.com.marcusferreira.voting.session.dto.VotingSessionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class VotingSessionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void openSessionReturns201WithClosesAtInTheFuture() {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta com sessão", null), AgendaResponse.class).getBody().id();

        ResponseEntity<VotingSessionResponse> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/sessoes",
            new OpenSessionRequest(120L),
            VotingSessionResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().closesAt()).isAfter(response.getBody().openedAt());
    }

    @Test
    void openSessionKeepsPortugueseJsonContract() {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta contrato sessão", null), AgendaResponse.class).getBody().id();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/sessoes", new OpenSessionRequest(120L), String.class);

        assertThat(response.getBody())
            .contains("\"pautaId\":" + agendaId)
            .contains("\"abertaEm\"")
            .contains("\"fechaEm\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-100", "86401", "10000000000000", "9223372036854775807"})
    void openSessionWithOutOfRangeDurationReturns400AndKeepsAgendaUsable(String duration) {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta com duração inválida " + duration, null), AgendaResponse.class).getBody().id();

        ResponseEntity<String> rejected = restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            json("{\"duracaoSegundos\":" + duration + "}"), String.class);
        ResponseEntity<String> retry = restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            json("{\"duracaoSegundos\":60}"), String.class);

        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rejected.getBody()).contains("\"duracaoSegundos\"");
        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void openingSecondSessionForSameAgendaReturns409() {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta duplicada", null), AgendaResponse.class).getBody().id();
        restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            new OpenSessionRequest(120L), VotingSessionResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/sessoes", new OpenSessionRequest(120L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void openingNewSessionAfterPreviousSessionClosedReturns409() throws InterruptedException {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta com sessão encerrada", null), AgendaResponse.class).getBody().id();
        ResponseEntity<VotingSessionResponse> first = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/sessoes", new OpenSessionRequest(1L), VotingSessionResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Make sure the first session is actually closed before the new attempt.
        Thread.sleep(1500);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/sessoes", new OpenSessionRequest(120L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
