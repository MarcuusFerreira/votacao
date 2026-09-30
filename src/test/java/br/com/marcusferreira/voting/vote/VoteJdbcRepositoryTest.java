package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class VoteJdbcRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    VoteJdbcRepository voteRepository;

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

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
    void votesTableHasNoIndexRedundantWithTheUniqueConstraint() {
        // (session_id, member_id) already serves lookups by session_id; a separate index on
        // session_id would only add write cost to every vote.
        List<String> indexes = jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'votes'", Map.of(), String.class);

        assertThat(indexes).containsExactlyInAnyOrder("votes_pkey", "uk_votes_session_member");
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
    void insertIntoOpenSessionInsertsWhenSessionIsOpen() {
        Long agendaId = agendaService.create("Pauta com sessão aberta", null).getId();
        Long sessionId = votingSessionService.open(agendaId, Duration.ofSeconds(60)).getId();

        boolean inserted = voteRepository.insertIntoOpenSession(agendaId, "associado-aberta", VoteOption.YES);

        assertThat(inserted).isTrue();
        assertThat(voteRepository.count(sessionId).yesVotes()).isEqualTo(1);
    }

    @Test
    void insertIntoOpenSessionSkipsWhenSessionIsClosed() {
        Long agendaId = agendaService.create("Pauta com sessão encerrada", null).getId();
        Long sessionId = votingSessionService.open(agendaId, Duration.ofSeconds(-1)).getId();

        boolean inserted = voteRepository.insertIntoOpenSession(agendaId, "associado-fechada", VoteOption.YES);

        assertThat(inserted).isFalse();
        assertThat(voteRepository.count(sessionId).yesVotes()).isZero();
    }

    @Test
    void insertIntoOpenSessionSkipsWhenAgendaHasNoSession() {
        Long agendaId = agendaService.create("Pauta sem sessão", null).getId();

        assertThat(voteRepository.insertIntoOpenSession(agendaId, "associado-sem-sessao", VoteOption.YES)).isFalse();
    }

    @Test
    void insertIntoOpenSessionThrowsOnDuplicateVote() {
        Long agendaId = agendaService.create("Pauta com voto duplicado", null).getId();
        votingSessionService.open(agendaId, Duration.ofSeconds(60));
        voteRepository.insertIntoOpenSession(agendaId, "associado-duplicado", VoteOption.YES);

        assertThatThrownBy(() -> voteRepository.insertIntoOpenSession(agendaId, "associado-duplicado", VoteOption.NO))
            .isInstanceOf(DuplicateVoteException.class);
    }

    @Test
    void insertingDuplicateVoteThrows() {
        Long sessionId = newSessionId();
        voteRepository.insert(sessionId, "associado-x", VoteOption.YES);

        assertThatThrownBy(() -> voteRepository.insert(sessionId, "associado-x", VoteOption.NO))
            .isInstanceOf(DuplicateVoteException.class);
    }
}
