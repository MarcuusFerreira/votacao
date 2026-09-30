package br.com.marcusferreira.voting.session;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {

    // voting_sessions.agenda_id is unique (V2): an agenda has at most one session.
    Optional<VotingSession> findByAgendaId(Long agendaId);
}
