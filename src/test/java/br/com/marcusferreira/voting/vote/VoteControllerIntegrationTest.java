package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.member.MemberEligibilityClient;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// CPF verification (bonus 1) is disabled by default in application.yaml; it is enabled here
// to exercise the full path with a mocked MemberEligibilityClient.
@TestPropertySource(properties = "voting.member.verification-enabled=true")
class VoteControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    MemberEligibilityClient memberEligibilityClient;

    private Long agendaWithOpenSession(String title) {
        Long agendaId = createAgenda(title);
        openSession(agendaId, 120);
        return agendaId;
    }

    @Test
    void successfulVoteReturns201WithConfirmationScreen() {
        Long agendaId = agendaWithOpenSession("Pauta votável");

        ResponseEntity<String> response = castVote(agendaId, "associado-1", "12345678900", "NAO");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(JsonPath.<String>read(response.getBody(), "$.titulo")).isEqualTo("Voto registrado");
        assertThat(JsonPath.<String>read(response.getBody(), "$.itens[0].texto")).contains("Não");
        assertThat(JsonPath.<String>read(response.getBody(), "$.botaoOk.url")).endsWith("/api/v1/pautas/" + agendaId + "/resultado");
    }

    @Test
    void voteFormCollectsMemberIdAndCpfAndCarriesTheChosenOption() {
        Long agendaId = agendaWithOpenSession("Pauta com formulário de voto");

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos/formulario", json("{\"voto\":\"SIM\"}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(response.getBody(), "$.itens[?(@.tipo == 'INPUT_TEXTO')].id"))
            .containsExactly("associadoId", "cpf");
        assertThat(JsonPath.<String>read(response.getBody(), "$.botaoOk.body.voto")).isEqualTo("SIM");
        assertThat(JsonPath.<String>read(response.getBody(), "$.botaoOk.url")).endsWith("/api/v1/pautas/" + agendaId + "/votos");
    }

    @Test
    void voteFormForClosedSessionReturns409() {
        Long agendaId = createAgenda("Pauta com formulário fechado");
        openSession(agendaId, 1);
        clock.advance(Duration.ofSeconds(2));

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos/formulario", json("{\"voto\":\"SIM\"}"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void votingTwiceWithSameMemberReturns409() {
        Long agendaId = agendaWithOpenSession("Pauta voto duplicado");
        castVote(agendaId, "associado-2", "12345678900", "SIM");

        assertThat(castVote(agendaId, "associado-2", "12345678900", "NAO").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void resultAfterSessionClosesShowsCount() {
        Long agendaId = createAgenda("Pauta com resultado");
        openSession(agendaId, 1);
        castVote(agendaId, "associado-r1", "12345678900", "SIM");

        clock.advance(Duration.ofSeconds(2));

        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/pautas/" + agendaId + "/resultado", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Sim: 1").contains("Vencedor: SIM");
        assertThat(JsonPath.<String>read(response.getBody(), "$.botaoOk.texto")).isEqualTo("Voltar às pautas");
    }

    @Test
    void concurrentVotesBeyondConnectionPoolSizeAreAllAccepted() throws Exception {
        Long agendaId = agendaWithOpenSession("Pauta com votos concorrentes");

        // More concurrent requests than the test pool size (10): each vote must hold at most
        // one connection at a time, otherwise the requests starve the pool.
        int members = 30;
        ExecutorService executor = Executors.newFixedThreadPool(members);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatusCode>> responses = new ArrayList<>();
        for (int i = 0; i < members; i++) {
            String memberId = "associado-concorrente-" + i;
            String cpf = "%011d".formatted(i);
            responses.add(executor.submit(() -> {
                start.await();
                return castVote(agendaId, memberId, cpf, "SIM").getStatusCode();
            }));
        }

        start.countDown();
        try {
            for (Future<HttpStatusCode> response : responses) {
                assertThat(response.get(20, TimeUnit.SECONDS)).isEqualTo(HttpStatus.CREATED);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void votingAfterSessionClosedReturns409() {
        Long agendaId = createAgenda("Pauta com sessão já encerrada");
        openSession(agendaId, 1);
        clock.advance(Duration.ofSeconds(2));

        assertThat(castVote(agendaId, "associado-atrasado", "12345678900", "SIM").getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void votingWithoutSessionReturns404() {
        Long agendaId = createAgenda("Pauta sem sessão para voto");

        assertThat(castVote(agendaId, "associado-sem-sessao", "12345678900", "SIM").getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resultWithoutVotesSaysNoVotesWereCast() {
        Long agendaId = agendaWithOpenSession("Pauta sem votos");
        clock.advance(Duration.ofSeconds(121));

        String body = restTemplate.getForObject("/api/v1/pautas/" + agendaId + "/resultado", String.class);

        assertThat(body).contains("Nenhum voto registrado").doesNotContain("EMPATE");
    }

    @Test
    void memberIdLongerThan64CharactersReturns400() {
        Long agendaId = agendaWithOpenSession("Pauta associadoId longo");

        ResponseEntity<String> response = castVote(agendaId, "a".repeat(65), "12345678900", "SIM");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"associadoId\":");
        Mockito.verifyNoInteractions(memberEligibilityClient);
    }
}
