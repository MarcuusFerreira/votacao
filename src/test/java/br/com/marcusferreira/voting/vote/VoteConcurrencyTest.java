package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            // Same member, distinct CPFs: only the (session, member) uniqueness can reject them.
            String cpf = "%011d".formatted(i);
            executor.submit(() -> {
                try {
                    start.await();
                    if (voteRepository.insertIntoOpenSession(agendaId, "associado-concorrente", cpf, VoteOption.YES)) {
                        successes.incrementAndGet();
                    }
                } catch (Throwable e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures).hasSize(threads - 1).allMatch(DuplicateVoteException.class::isInstance);
        assertThat(voteRepository.count(session.getId()).yesVotes()).isEqualTo(1);
    }
}
