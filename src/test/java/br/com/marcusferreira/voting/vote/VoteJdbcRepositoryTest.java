package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.common.exception.CpfAlreadyUsedException;
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

    @Test
    void votesTableHasNoIndexRedundantWithTheUniqueConstraint() {
        // (session_id, member_id) already serves lookups by session_id; a separate index on
        // session_id would only add write cost to every vote.
        List<String> indexes = jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'votes'", Map.of(), String.class);

        assertThat(indexes).containsExactlyInAnyOrder("votes_pkey", "uk_votes_session_member", "uk_votes_session_cpf");
    }

    @Test
    void insertAndCountAggregatesByOption() {
        Long agendaId = agendaService.create("Pauta para voto", null).getId();
        Long sessionId = votingSessionService.open(agendaId, Duration.ofSeconds(60)).getId();

        voteRepository.insertIntoOpenSession(agendaId, "associado-1", "00000000001", VoteOption.YES);
        voteRepository.insertIntoOpenSession(agendaId, "associado-2", "00000000002", VoteOption.YES);
        voteRepository.insertIntoOpenSession(agendaId, "associado-3", "00000000003", VoteOption.NO);

        VotingResult result = voteRepository.count(sessionId);

        assertThat(result.yesVotes()).isEqualTo(2);
        assertThat(result.noVotes()).isEqualTo(1);
        assertThat(result.winner()).isEqualTo(VotingResult.Winner.YES);
    }

    @Test
    void insertIntoOpenSessionInsertsWhenSessionIsOpen() {
        Long agendaId = agendaService.create("Pauta com sessão aberta", null).getId();
        Long sessionId = votingSessionService.open(agendaId, Duration.ofSeconds(60)).getId();

        boolean inserted = voteRepository.insertIntoOpenSession(agendaId, "associado-aberta", "00000000004", VoteOption.YES);

        assertThat(inserted).isTrue();
        assertThat(voteRepository.count(sessionId).yesVotes()).isEqualTo(1);
    }

    @Test
    void insertIntoOpenSessionSkipsWhenSessionIsClosed() {
        Long agendaId = agendaService.create("Pauta com sessão encerrada", null).getId();
        Long sessionId = votingSessionService.open(agendaId, Duration.ofSeconds(-1)).getId();

        boolean inserted = voteRepository.insertIntoOpenSession(agendaId, "associado-fechada", "00000000005", VoteOption.YES);

        assertThat(inserted).isFalse();
        assertThat(voteRepository.count(sessionId).yesVotes()).isZero();
    }

    @Test
    void insertIntoOpenSessionSkipsWhenAgendaHasNoSession() {
        Long agendaId = agendaService.create("Pauta sem sessão", null).getId();

        assertThat(voteRepository.insertIntoOpenSession(agendaId, "associado-sem-sessao", "00000000006", VoteOption.YES)).isFalse();
    }

    @Test
    void insertIntoOpenSessionRejectsSameCpfForAnotherMember() {
        Long agendaId = agendaService.create("Pauta com CPF repetido", null).getId();
        votingSessionService.open(agendaId, Duration.ofSeconds(60));
        voteRepository.insertIntoOpenSession(agendaId, "associado-original", "98765432100", VoteOption.YES);

        assertThatThrownBy(() -> voteRepository.insertIntoOpenSession(agendaId, "outro-associado", "98765432100", VoteOption.NO))
            .isInstanceOf(CpfAlreadyUsedException.class);
    }

    @Test
    void insertIntoOpenSessionThrowsOnDuplicateVote() {
        Long agendaId = agendaService.create("Pauta com voto duplicado", null).getId();
        votingSessionService.open(agendaId, Duration.ofSeconds(60));
        voteRepository.insertIntoOpenSession(agendaId, "associado-duplicado", "00000000007", VoteOption.YES);

        assertThatThrownBy(() -> voteRepository.insertIntoOpenSession(agendaId, "associado-duplicado", "00000000007", VoteOption.NO))
            .isInstanceOf(DuplicateVoteException.class);
    }
}
