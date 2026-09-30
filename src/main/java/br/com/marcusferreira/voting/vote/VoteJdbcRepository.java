package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VoteJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public VoteJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Long sessionId, String memberId, VoteOption vote) {
        String sql = """
            INSERT INTO votes (session_id, member_id, vote, created_at)
            VALUES (:sessionId, :memberId, :vote, :createdAt)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("sessionId", sessionId)
            .addValue("memberId", memberId)
            .addValue("vote", vote.name())
            .addValue("createdAt", Timestamp.from(Instant.now()));
        try {
            jdbcTemplate.update(sql, params);
        } catch (DuplicateKeyException e) {
            throw new DuplicateVoteException(sessionId, memberId, e);
        }
    }

    /**
     * Records the vote in the agenda's session in a single statement, as long as the session is
     * still open. Returns {@code false} when nothing was inserted: the agenda has no session or
     * it is already closed.
     */
    public boolean insertIntoOpenSession(Long agendaId, String memberId, VoteOption vote) {
        String sql = """
            INSERT INTO votes (session_id, member_id, vote, created_at)
            SELECT id, :memberId, :vote, :now
            FROM voting_sessions
            WHERE agenda_id = :agendaId AND closes_at > :now
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("agendaId", agendaId)
            .addValue("memberId", memberId)
            .addValue("vote", vote.name())
            .addValue("now", Timestamp.from(Instant.now()));
        try {
            return jdbcTemplate.update(sql, params) == 1;
        } catch (DuplicateKeyException e) {
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
