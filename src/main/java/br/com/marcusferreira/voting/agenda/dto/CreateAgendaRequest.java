package br.com.marcusferreira.voting.agenda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateAgendaRequest(
    @JsonProperty("titulo")
    @NotBlank(message = "titulo é obrigatório")
    String title,
    @JsonProperty("descricao")
    String description
) {
}
