package br.com.marcusferreira.voting.session.dto;

import br.com.marcusferreira.voting.session.VotingSession;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record VotingSessionResponse(
    Long id,
    @JsonProperty("pautaId") Long agendaId,
    @JsonProperty("abertaEm") Instant openedAt,
    @JsonProperty("fechaEm") Instant closesAt
) {

    public static VotingSessionResponse from(VotingSession session) {
        return new VotingSessionResponse(session.getId(), session.getAgendaId(), session.getOpenedAt(), session.getClosesAt());
    }
}
