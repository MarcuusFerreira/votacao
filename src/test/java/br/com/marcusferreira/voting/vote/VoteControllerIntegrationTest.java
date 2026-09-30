package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import br.com.marcusferreira.voting.member.MemberEligibilityClient;
import br.com.marcusferreira.voting.session.dto.OpenSessionRequest;
import br.com.marcusferreira.voting.session.dto.VotingSessionResponse;
import br.com.marcusferreira.voting.vote.dto.CastVoteRequest;
import br.com.marcusferreira.voting.vote.dto.VoteResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// CPF verification (bonus 1) is disabled by default in application.yaml; it is enabled here
// to exercise the full path with a mocked MemberEligibilityClient.
@TestPropertySource(properties = "voting.member.verification-enabled=true")
class VoteControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @MockitoBean
    MemberEligibilityClient memberEligibilityClient;

    private Long agendaWithOpenSession(String title) {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest(title, null), AgendaResponse.class).getBody().id();
        restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            new OpenSessionRequest(120L), VotingSessionResponse.class);
        return agendaId;
    }

    @Test
    void successfulVoteReturns201() {
        Mockito.doNothing().when(memberEligibilityClient).checkEligibility(Mockito.anyString());
        Long agendaId = agendaWithOpenSession("Pauta votável");

        ResponseEntity<VoteResponse> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos",
            new CastVoteRequest("associado-1", "12345678900", VoteOption.YES),
            VoteResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void voteKeepsPortugueseJsonContract() {
        Mockito.doNothing().when(memberEligibilityClient).checkEligibility(Mockito.anyString());
        Long agendaId = agendaWithOpenSession("Pauta contrato voto");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos",
            new HttpEntity<>("{\"associadoId\":\"associado-json\",\"cpf\":\"12345678900\",\"voto\":\"NAO\"}", headers),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
            .contains("\"pautaId\":" + agendaId)
            .contains("\"associadoId\":\"associado-json\"")
            .contains("\"voto\":\"NAO\"");
    }

    @Test
    void votingTwiceWithSameMemberReturns409() {
        Mockito.doNothing().when(memberEligibilityClient).checkEligibility(Mockito.anyString());
        Long agendaId = agendaWithOpenSession("Pauta voto duplicado");
        restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/votos",
            new CastVoteRequest("associado-2", "12345678900", VoteOption.YES), VoteResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos",
            new CastVoteRequest("associado-2", "12345678900", VoteOption.NO),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void resultAfterSessionClosesShowsCount() {
        Mockito.doNothing().when(memberEligibilityClient).checkEligibility(Mockito.anyString());
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta com resultado", null), AgendaResponse.class).getBody().id();
        restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            new OpenSessionRequest(1L), VotingSessionResponse.class);
        restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/votos",
            new CastVoteRequest("associado-r1", "12345678900", VoteOption.YES), VoteResponse.class);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/pautas/" + agendaId + "/resultado", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Sim: 1").contains("Vencedor: SIM");
    }

    @Test
    void concurrentVotesBeyondConnectionPoolSizeAreAllAccepted() throws Exception {
        Mockito.doNothing().when(memberEligibilityClient).checkEligibility(Mockito.anyString());
        Long agendaId = agendaWithOpenSession("Pauta com votos concorrentes");

        // More concurrent requests than the default pool size (10): each vote must hold at
        // most one connection at a time, otherwise the requests starve the pool.
        int members = 30;
        ExecutorService executor = Executors.newFixedThreadPool(members);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatusCode>> responses = new ArrayList<>();
        for (int i = 0; i < members; i++) {
            CastVoteRequest request = new CastVoteRequest("associado-concorrente-" + i, "12345678900", VoteOption.YES);
            responses.add(executor.submit(() -> {
                start.await();
                return restTemplate.postForEntity(
                    "/api/v1/pautas/" + agendaId + "/votos", request, String.class).getStatusCode();
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
    void votingAfterSessionClosedReturns409() throws InterruptedException {
        Mockito.doNothing().when(memberEligibilityClient).checkEligibility(Mockito.anyString());
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta com sessão já encerrada", null), AgendaResponse.class).getBody().id();
        restTemplate.postForEntity("/api/v1/pautas/" + agendaId + "/sessoes",
            new OpenSessionRequest(1L), VotingSessionResponse.class);
        Thread.sleep(1500);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos",
            new CastVoteRequest("associado-atrasado", "12345678900", VoteOption.YES),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void votingWithoutSessionReturns404() {
        Long agendaId = restTemplate.postForEntity("/api/v1/pautas",
            new CreateAgendaRequest("Pauta sem sessão para voto", null), AgendaResponse.class).getBody().id();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos",
            new CastVoteRequest("associado-sem-sessao", "12345678900", VoteOption.YES),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void memberIdLongerThan64CharactersReturns400() {
        Long agendaId = agendaWithOpenSession("Pauta associadoId longo");

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/pautas/" + agendaId + "/votos",
            new CastVoteRequest("a".repeat(65), "12345678900", VoteOption.YES),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"associadoId\":");
        Mockito.verifyNoInteractions(memberEligibilityClient);
    }
}
