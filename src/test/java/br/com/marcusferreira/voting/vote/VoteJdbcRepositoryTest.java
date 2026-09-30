package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VoteJdbcRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    VoteJdbcRepository voteRepository;

    @Autowired
    AgendaService agendaService;

    @Autowired
    VotingSessionService votingSessionService;

    private Long newSessionId() {
        Long agendaId = agendaService.create("Pauta para voto", null).getId();
        VotingSession session = votingSessionService.open(agendaId, Duration.ofSeconds(60));
        return session.getId();
    }

    @Test
    void insertAndCountAggregatesByOption() {
        Long sessionId = newSessionId();

        voteRepository.insert(sessionId, "associado-1", VoteOption.YES);
        voteRepository.insert(sessionId, "associado-2", VoteOption.YES);
        voteRepository.insert(sessionId, "associado-3", VoteOption.NO);

        VotingResult result = voteRepository.count(sessionId);

        assertThat(result.yesVotes()).isEqualTo(2);
        assertThat(result.noVotes()).isEqualTo(1);
        assertThat(result.winner()).isEqualTo(VotingResult.Winner.YES);
    }

    @Test
    void insertingDuplicateVoteThrows() {
        Long sessionId = newSessionId();
        voteRepository.insert(sessionId, "associado-x", VoteOption.YES);

        assertThatThrownBy(() -> voteRepository.insert(sessionId, "associado-x", VoteOption.NO))
            .isInstanceOf(DuplicateVoteException.class);
    }
}
