package br.com.marcusferreira.voting.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "voting_sessions")
public class VotingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agenda_id", nullable = false)
    private Long agendaId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    protected VotingSession() {
    }

    public VotingSession(Long agendaId, Instant openedAt, Duration duration) {
        this.agendaId = agendaId;
        this.openedAt = openedAt;
        this.closesAt = this.openedAt.plus(duration);
    }

    public boolean isOpen(Instant now) {
        return now.isBefore(closesAt);
    }

    public Long getId() {
        return id;
    }

    public Long getAgendaId() {
        return agendaId;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }
}
