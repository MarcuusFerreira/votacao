package br.com.marcusferreira.voting.session;

import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.common.ConstraintViolations;
import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.SessionAlreadyOpenException;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class VotingSessionService {

    private static final String SINGLE_SESSION_CONSTRAINT = "uk_voting_sessions_agenda";

    private final VotingSessionRepository repository;
    private final AgendaService agendaService;
    private final VotingProperties properties;
    private final Clock clock;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VotingSessionService.class);

    public VotingSessionService(VotingSessionRepository repository, AgendaService agendaService,
                                VotingProperties properties, Clock clock) {
        this.repository = repository;
        this.agendaService = agendaService;
        this.properties = properties;
        this.clock = clock;
    }

    public VotingSession open(Long agendaId, Duration requestedDuration) {
        agendaService.findById(agendaId);
        findSession(agendaId)
            .filter(this::isOpen)
            .ifPresent(session -> {
                throw new SessionAlreadyOpenException(agendaId);
            });
        Duration duration = requestedDuration != null ? requestedDuration : properties.session().defaultDuration();
        VotingSession session;
        try {
            // The unique constraint on voting_sessions(agenda_id) (V2) is the definitive guarantee of a
            // single session per agenda: it covers already-closed sessions and concurrent openings,
            // cases the in-memory check above cannot detect.
            session = repository.save(new VotingSession(agendaId, clock.instant(), duration));
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.violates(e, SINGLE_SESSION_CONSTRAINT)) {
                throw new SessionAlreadyOpenException(agendaId, e);
            }
            throw e;
        }
        log.info("Voting session opened: agendaId={} sessionId={} closesAt={}", agendaId, session.getId(), session.getClosesAt());
        return session;
    }

    public boolean isOpen(VotingSession session) {
        return session.isOpen(clock.instant());
    }

    public Optional<VotingSession> findSession(Long agendaId) {
        return repository.findByAgendaId(agendaId);
    }

    /**
     * The agenda's session; a missing agenda is reported as such rather than as a missing session.
     */
    public VotingSession getSessionOrThrow(Long agendaId) {
        return findSession(agendaId).orElseThrow(() -> {
            agendaService.findById(agendaId);
            return new SessionNotFoundException(agendaId);
        });
    }

    public VotingSession getOpenSessionOrThrow(Long agendaId) {
        VotingSession session = getSessionOrThrow(agendaId);
        if (!isOpen(session)) {
            throw new SessionClosedException(agendaId);
        }
        return session;
    }
}
