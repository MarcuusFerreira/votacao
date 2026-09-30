package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VoteConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    VoteJdbcRepository voteRepository;

    @Autowired
    AgendaService agendaService;

    @Autowired
    VotingSessionService votingSessionService;

    @Test
    void onlyOneVotePersistsWhenSameMemberVotesConcurrently() throws InterruptedException {
        Long agendaId = agendaService.create("Pauta concorrente", null).getId();
        VotingSession session = votingSessionService.open(agendaId, Duration.ofSeconds(60));

        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    voteRepository.insert(session.getId(), "associado-concorrente", VoteOption.YES);
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // expected: only one thread should manage to insert
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(voteRepository.count(session.getId()).yesVotes()).isEqualTo(1);
    }
}
