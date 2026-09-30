package br.com.marcusferreira.voting.vote.dto;

import br.com.marcusferreira.voting.vote.VoteOption;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CastVoteRequest(
    @JsonProperty("associadoId")
    @NotBlank(message = "associadoId é obrigatório")
    @Size(max = 64, message = "associadoId deve ter no máximo 64 caracteres")
    String memberId,
    @NotBlank(message = "cpf é obrigatório")
    String cpf,
    @JsonProperty("voto")
    @NotNull(message = "voto é obrigatório")
    VoteOption vote
) {
}
