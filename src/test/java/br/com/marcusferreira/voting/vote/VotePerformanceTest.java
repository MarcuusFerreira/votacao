package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.session.VotingSessionService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Tag("performance")
class VotePerformanceTest extends AbstractIntegrationTest {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    AgendaService agendaService;

    @Autowired
    VotingSessionService votingSessionService;

    @Autowired
    VoteJdbcRepository voteRepository;

    @Test
    void insertsAndCountsOneHundredThousandVotesInReasonableTime() {
        Long agendaId = agendaService.create("Pauta de performance", null).getId();
        Long sessionId = votingSessionService.open(agendaId, Duration.ofMinutes(10)).getId();

        int total = 100_000;
        List<MapSqlParameterSource> batch = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            batch.add(new MapSqlParameterSource()
                .addValue("sessionId", sessionId)
                .addValue("memberId", "associado-" + i)
                .addValue("vote", (i % 2 == 0 ? VoteOption.YES : VoteOption.NO).name())
                .addValue("createdAt", Timestamp.from(Instant.now())));
        }

        String insertSql = """
            INSERT INTO votes (session_id, member_id, vote, created_at)
            VALUES (:sessionId, :memberId, :vote, :createdAt)
            """;

        long insertStart = System.nanoTime();
        jdbcTemplate.batchUpdate(insertSql, batch.toArray(new MapSqlParameterSource[0]));
        long insertMillis = (System.nanoTime() - insertStart) / 1_000_000;

        long countStart = System.nanoTime();
        VotingResult result = voteRepository.count(sessionId);
        long countMillis = (System.nanoTime() - countStart) / 1_000_000;

        assertThat(result.yesVotes() + result.noVotes()).isEqualTo(total);
        System.out.printf("Inserted %d votes: %dms | Aggregation: %dms%n", total, insertMillis, countMillis);
        assertThat(countMillis).isLessThan(2_000);
    }
}
