package br.com.marcusferreira.voting.session;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {

    Optional<VotingSession> findFirstByAgendaIdOrderByIdDesc(Long agendaId);
}
