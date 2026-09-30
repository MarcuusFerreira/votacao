package br.com.marcusferreira.voting.session;

import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.SessionAlreadyOpenException;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
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
        findCurrentSession(agendaId)
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
            if (violates(e, SINGLE_SESSION_CONSTRAINT)) {
                throw new SessionAlreadyOpenException(agendaId, e);
            }
            throw e;
        }
        log.info("Voting session opened: agendaId={} sessionId={} closesAt={}", agendaId, session.getId(), session.getClosesAt());
        return session;
    }

    private static boolean violates(DataIntegrityViolationException e, String constraint) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return constraint.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }

    public boolean isOpen(VotingSession session) {
        return session.isOpen(clock.instant());
    }

    public Optional<VotingSession> findCurrentSession(Long agendaId) {
        return repository.findFirstByAgendaIdOrderByIdDesc(agendaId);
    }

    public VotingSession getOpenSessionOrThrow(Long agendaId) {
        VotingSession session = findCurrentSession(agendaId)
            .orElseThrow(() -> new SessionNotFoundException(agendaId));
        if (!isOpen(session)) {
            throw new SessionClosedException(agendaId);
        }
        return session;
    }
}
