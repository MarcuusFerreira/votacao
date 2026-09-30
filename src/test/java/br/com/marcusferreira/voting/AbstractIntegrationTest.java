package br.com.marcusferreira.voting;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(AbstractIntegrationTest.ClockConfiguration.class)
public abstract class AbstractIntegrationTest {

    @TestConfiguration
    static class ClockConfiguration {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /** Creates an agenda through the API and returns its id, taken from the Location header. */
    protected Long createAgenda(String title) {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/pautas",
            json("{\"titulo\":\"" + title + "\"}"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = response.getHeaders().getLocation().getPath();
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    protected ResponseEntity<String> openSession(Long agendaId, long durationSeconds) {
        return restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            json("{\"duracaoSegundos\":" + durationSeconds + "}"), String.class);
    }

    protected ResponseEntity<String> castVote(Long agendaId, String memberId, String cpf, String vote) {
        return restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/votos",
            json("{\"associadoId\":\"" + memberId + "\",\"cpf\":\"" + cpf + "\",\"voto\":\"" + vote + "\"}"),
            String.class);
    }

    // Singleton container pattern: this base class is extended by multiple concrete
    // integration test classes. Using @Testcontainers/@Container would make JUnit's
    // extension start/stop this same static container instance once per subclass,
    // and Testcontainers does not reliably support restarting an already-stopped
    // container. Starting it once here (reused for the whole JVM, reaped by Ryuk on
    // exit) avoids that.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Each distinct test context keeps its own pool open against the shared container
        // (max_connections=100), so tests use Hikari's default size instead of the production one.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 10);
    }
}
