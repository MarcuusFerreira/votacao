package br.com.marcusferreira.voting.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.marcusferreira.voting.agenda.Agenda;
import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.SessionAlreadyOpenException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VotingSessionServiceTest {

    @Mock
    VotingSessionRepository repository;

    @Mock
    AgendaService agendaService;

    private static VotingProperties properties() {
        return new VotingProperties(
            new VotingProperties.Session(Duration.ofSeconds(60)),
            new VotingProperties.Member("http://example.com", false));
    }

    @Test
    void openUsesDefaultDurationWhenNotProvided() {
        VotingSessionService service = new VotingSessionService(repository, agendaService, properties());

        when(agendaService.findById(1L)).thenReturn(new Agenda("Pauta", null));
        when(repository.findFirstByAgendaIdOrderByIdDesc(1L)).thenReturn(Optional.empty());
        when(repository.save(any(VotingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        VotingSession session = service.open(1L, null);

        assertThat(session.getClosesAt()).isAfter(Instant.now().plusSeconds(50));
    }

    @Test
    void openThrowsWhenSessionAlreadyOpen() {
        VotingSessionService service = new VotingSessionService(repository, agendaService, properties());

        when(agendaService.findById(1L)).thenReturn(new Agenda("Pauta", null));
        VotingSession open = new VotingSession(1L, Duration.ofSeconds(60));
        when(repository.findFirstByAgendaIdOrderByIdDesc(1L)).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> service.open(1L, null))
            .isInstanceOf(SessionAlreadyOpenException.class);
    }

    @Test
    void openTranslatesUniqueViolationToSessionAlreadyOpenWhenPreviousSessionClosed() {
        VotingSessionService service = new VotingSessionService(repository, agendaService, properties());

        when(agendaService.findById(1L)).thenReturn(new Agenda("Pauta", null));
        VotingSession closed = new VotingSession(1L, Duration.ofSeconds(-1));
        when(repository.findFirstByAgendaIdOrderByIdDesc(1L)).thenReturn(Optional.of(closed));
        DataIntegrityViolationException violation = violationOf("uk_voting_sessions_agenda");
        when(repository.save(any(VotingSession.class))).thenThrow(violation);

        assertThatThrownBy(() -> service.open(1L, null))
            .isInstanceOf(SessionAlreadyOpenException.class)
            .hasCause(violation);
    }

    @Test
    void openRethrowsIntegrityViolationsOtherThanTheSingleSessionConstraint() {
        VotingSessionService service = new VotingSessionService(repository, agendaService, properties());

        when(agendaService.findById(1L)).thenReturn(new Agenda("Pauta", null));
        when(repository.findFirstByAgendaIdOrderByIdDesc(1L)).thenReturn(Optional.empty());
        DataIntegrityViolationException violation = violationOf("voting_sessions_agenda_id_fkey");
        when(repository.save(any(VotingSession.class))).thenThrow(violation);

        assertThatThrownBy(() -> service.open(1L, null)).isSameAs(violation);
    }

    private static DataIntegrityViolationException violationOf(String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
            new ConstraintViolationException("violation", new SQLException("violation"), constraint));
    }
}
