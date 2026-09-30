package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.common.exception.CpfAlreadyUsedException;
import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VoteJdbcRepository {

    private static final String CPF_CONSTRAINT = "uk_votes_session_cpf";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    public VoteJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    /**
     * Records the vote in the agenda's session in a single statement, as long as the session is
     * still open. Returns {@code false} when nothing was inserted: the agenda has no session or
     * it is already closed.
     */ 
    public boolean insertIntoOpenSession(Long agendaId, String memberId, String cpf, VoteOption vote) {
        String sql = """
            INSERT INTO votes (session_id, member_id, cpf, vote, created_at)
            SELECT id, :memberId, :cpf, :vote, :now
            FROM voting_sessions
            WHERE agenda_id = :agendaId AND closes_at > :now
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("agendaId", agendaId)
            .addValue("memberId", memberId)
            .addValue("cpf", cpf)
            .addValue("vote", vote.name())
            .addValue("now", Timestamp.from(clock.instant()));
        try {
            return jdbcTemplate.update(sql, params) == 1;
        } catch (DuplicateKeyException e) {
            if (e.getMessage() != null && e.getMessage().contains(CPF_CONSTRAINT)) {
                throw new CpfAlreadyUsedException(cpf, e);
            }
            throw new DuplicateVoteException(sessionIdOf(agendaId), memberId, e);
        }
    }

    private Long sessionIdOf(Long agendaId) {
        return jdbcTemplate.queryForObject("SELECT id FROM voting_sessions WHERE agenda_id = :agendaId",
            new MapSqlParameterSource("agendaId", agendaId), Long.class);
    }

    public VotingResult count(Long sessionId) {
        String sql = "SELECT vote, COUNT(*) AS total FROM votes WHERE session_id = :sessionId GROUP BY vote";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("sessionId", sessionId);
        Map<String, Long> totals = new HashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            totals.put(rs.getString("vote"), rs.getLong("total"));
        });
        return new VotingResult(
            totals.getOrDefault(VoteOption.YES.name(), 0L),
            totals.getOrDefault(VoteOption.NO.name(), 0L));
    }
}
