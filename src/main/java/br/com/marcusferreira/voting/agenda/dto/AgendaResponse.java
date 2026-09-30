package br.com.marcusferreira.voting.agenda.dto;

import br.com.marcusferreira.voting.agenda.Agenda;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record AgendaResponse(
    Long id,
    @JsonProperty("titulo") String title,
    @JsonProperty("descricao") String description,
    @JsonProperty("criadaEm") Instant createdAt
) {

    public static AgendaResponse from(Agenda agenda) {
        return new AgendaResponse(agenda.getId(), agenda.getTitle(), agenda.getDescription(), agenda.getCreatedAt());
    }
}
