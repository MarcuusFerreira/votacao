package br.com.marcusferreira.voting.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
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
}
