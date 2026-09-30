package br.com.marcusferreira.voting.screen;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

// Emulator scenario: the Android emulator reaches the host machine through 10.0.2.2.
@TestPropertySource(properties = "voting.public-base-url=http://10.0.2.2:8080")
class ScreenBaseUrlIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void screensPointToTheConfiguredPublicBaseUrl() {
        ResponseEntity<AgendaResponse> created = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta no emulador", null), AgendaResponse.class);
        Long agendaId = created.getBody().id();

        String list = restTemplate.getForObject("/api/v1/pautas", String.class);
        String detail = restTemplate.getForObject("/api/v1/pautas/" + agendaId, String.class);

        assertThat(JsonPath.<List<String>>read(list, "$.itens[*].url")).allMatch(url -> url.startsWith("http://10.0.2.2:8080/api/v1/"));
        assertThat(JsonPath.<String>read(detail, "$.botaoOk.url")).isEqualTo("http://10.0.2.2:8080/api/v1/pautas/" + agendaId + "/sessoes");
        assertThat(created.getHeaders().getLocation()).hasToString("http://10.0.2.2:8080/api/v1/pautas/" + agendaId);
    }
}
