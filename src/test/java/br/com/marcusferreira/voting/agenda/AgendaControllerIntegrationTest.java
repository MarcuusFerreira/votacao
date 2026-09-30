package br.com.marcusferreira.voting.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AgendaControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createAgendaReturns201WithLocationAndOpenSessionScreen() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/pautas",
            json("{\"titulo\":\"Pauta em português\",\"descricao\":\"desc\"}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(JsonPath.<String>read(response.getBody(), "$.tipo")).isEqualTo("FORMULARIO");
        assertThat(JsonPath.<String>read(response.getBody(), "$.titulo")).isEqualTo("Pauta em português");
        assertThat(JsonPath.<String>read(response.getBody(), "$.botaoOk.texto")).isEqualTo("Abrir sessão");
    }

    @Test
    void createAgendaWithoutTitleReturns400WithPortugueseFieldName() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/pautas",
            json("{\"titulo\":\"\",\"descricao\":\"Descrição\"}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"titulo\":\"titulo é obrigatório\"");
    }

    @Test
    void listReturnsSelectionScreenStartingWithNewAgendaItem() {
        createAgenda("Pauta para listagem");

        String body = restTemplate.getForObject("/api/v1/pautas", String.class);

        assertThat(JsonPath.<String>read(body, "$.tipo")).isEqualTo("SELECAO");
        assertThat(JsonPath.<List<String>>read(body, "$.itens[*].texto")).startsWith("Nova pauta").contains("Pauta para listagem");
    }

    @Test
    void listIsPaginatedNewestFirstWithNextPageItem() {
        for (String title : List.of("Paginada A", "Paginada B", "Paginada C")) {
            createAgenda(title);
        }

        String body = restTemplate.getForObject("/api/v1/pautas?pagina=0&tamanho=2", String.class);

        assertThat(JsonPath.<List<String>>read(body, "$.itens[*].texto"))
            .containsExactly("Nova pauta", "Paginada C", "Paginada B", "Próxima página");
        assertThat(JsonPath.<String>read(body, "$.itens[3].url")).contains("pagina=1").contains("tamanho=2");
    }

    @Test
    void nextPageIsReachableByPostAsTheAppDoes() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/pautas?pagina=0&tamanho=1", json("{}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<String>read(response.getBody(), "$.tipo")).isEqualTo("SELECAO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pagina=-1", "tamanho=0", "tamanho=101"})
    void listRejectsOutOfRangePagination(String query) {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/pautas?" + query, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void agendaDetailWithoutSessionAsksForSessionDuration() {
        Long agendaId = createAgenda("Pauta sem sessão");

        String body = restTemplate.getForObject("/api/v1/pautas/" + agendaId, String.class);

        assertThat(JsonPath.<String>read(body, "$.tipo")).isEqualTo("FORMULARIO");
        assertThat(JsonPath.<List<String>>read(body, "$.itens[?(@.tipo == 'INPUT_NUMERO')].id")).containsExactly("duracaoSegundos");
        assertThat(JsonPath.<String>read(body, "$.botaoOk.url")).endsWith("/api/v1/pautas/" + agendaId + "/sessoes");
    }

    @Test
    void agendaDetailWithOpenSessionOffersVoteOptionsLeadingToTheVoteForm() {
        Long agendaId = createAgenda("Pauta com sessão aberta");
        openSession(agendaId, 120);

        String body = restTemplate.postForObject("/api/v1/pautas/" + agendaId, json("{}"), String.class);

        assertThat(JsonPath.<String>read(body, "$.tipo")).isEqualTo("SELECAO");
        assertThat(JsonPath.<List<String>>read(body, "$.itens[*].body.voto")).containsExactly("SIM", "NAO");
        assertThat(JsonPath.<List<String>>read(body, "$.itens[*].url"))
            .allMatch(url -> url.endsWith("/api/v1/pautas/" + agendaId + "/votos/formulario"));
    }

    @Test
    void newAgendaFormCollectsTitleAndDescription() {
        String body = restTemplate.postForObject("/api/v1/pautas/formulario", json("{}"), String.class);

        assertThat(JsonPath.<List<String>>read(body, "$.itens[*].id")).containsExactly("titulo", "descricao");
        assertThat(JsonPath.<String>read(body, "$.botaoOk.url")).endsWith("/api/v1/pautas");
    }
}
