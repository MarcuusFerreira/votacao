package br.com.marcusferreira.voting.session;

import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.SessionAlreadyOpenException;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class VotingSessionService {

    private final VotingSessionRepository repository;
    private final AgendaService agendaService;
    private final VotingProperties properties;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VotingSessionService.class);

    public VotingSessionService(VotingSessionRepository repository, AgendaService agendaService,
                                VotingProperties properties) {
        this.repository = repository;
        this.agendaService = agendaService;
        this.properties = properties;
    }

    public VotingSession open(Long agendaId, Duration requestedDuration) {
        agendaService.findById(agendaId);
        findCurrentSession(agendaId)
            .filter(VotingSession::isOpen)
            .ifPresent(session -> {
                throw new SessionAlreadyOpenException(agendaId);
            });
        Duration duration = requestedDuration != null ? requestedDuration : properties.session().defaultDuration();
        VotingSession session;
        try {
            // The unique constraint on voting_sessions(agenda_id) (V2) is the definitive guarantee of a
            // single session per agenda: it covers already-closed sessions and concurrent openings,
            // cases the in-memory check above cannot detect.
            session = repository.save(new VotingSession(agendaId, duration));
        } catch (DataIntegrityViolationException e) {
            throw new SessionAlreadyOpenException(agendaId, e);
        }
        log.info("Voting session opened: agendaId={} sessionId={} closesAt={}", agendaId, session.getId(), session.getClosesAt());
        return session;
    }

    public Optional<VotingSession> findCurrentSession(Long agendaId) {
        return repository.findFirstByAgendaIdOrderByIdDesc(agendaId);
    }

    public VotingSession getOpenSessionOrThrow(Long agendaId) {
        VotingSession session = findCurrentSession(agendaId)
            .orElseThrow(() -> new SessionNotFoundException(agendaId));
        if (!session.isOpen()) {
            throw new SessionClosedException(agendaId);
        }
        return session;
    }
}
