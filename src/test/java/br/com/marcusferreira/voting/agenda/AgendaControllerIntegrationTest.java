package br.com.marcusferreira.voting.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import br.com.marcusferreira.voting.session.dto.OpenSessionRequest;
import br.com.marcusferreira.voting.session.dto.VotingSessionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AgendaControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void createAgendaReturns201WithCreatedResource() {
        CreateAgendaRequest request = new CreateAgendaRequest("Nova pauta", "Descrição da pauta");

        ResponseEntity<AgendaResponse> response =
            restTemplate.postForEntity("/api/v1/pautas", request, AgendaResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("Nova pauta");
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    void createAgendaKeepsPortugueseJsonContract() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/pautas",
            json("{\"titulo\":\"Pauta em português\",\"descricao\":\"desc\"}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
            .contains("\"titulo\":\"Pauta em português\"")
            .contains("\"descricao\":\"desc\"")
            .contains("\"criadaEm\"");
    }

    @Test
    void createAgendaWithoutTitleReturns400WithPortugueseFieldName() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/pautas",
            json("{\"titulo\":\"\",\"descricao\":\"Descrição\"}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"titulo\":\"titulo é obrigatório\"");
    }

    @Test
    void listAgendasReturnsSelectionScreen() {
        restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta para listagem", null), AgendaResponse.class);

        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/pautas", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"tipo\":\"SELECAO\"");
        assertThat(response.getBody()).contains("Pauta para listagem");
    }

    @Test
    void agendaDetailWithoutSessionOffersOpenSessionButton() {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta sem sessão", "desc"), AgendaResponse.class).getBody().id();

        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/pautas/" + agendaId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"tipo\":\"FORMULARIO\"");
        assertThat(response.getBody()).contains("Abrir sessão");
    }

    @Test
    void agendaDetailWithOpenSessionOffersVoteOptions() {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta com sessão aberta", "desc"), AgendaResponse.class).getBody().id();
        restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            new OpenSessionRequest(120L), VotingSessionResponse.class);

        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/pautas/" + agendaId, String.class);

        assertThat(response.getBody()).contains("\"tipo\":\"SELECAO\"");
        assertThat(response.getBody()).contains("\"voto\":\"SIM\"");
        assertThat(response.getBody()).contains("\"voto\":\"NAO\"");
    }
}
