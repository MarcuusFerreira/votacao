package br.com.marcusferreira.voting.vote.dto;

import br.com.marcusferreira.voting.vote.VoteOption;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record VoteFormRequest(
    @JsonProperty("voto")
    @NotNull(message = "voto é obrigatório")
    VoteOption vote
) {
}
