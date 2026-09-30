package br.com.marcusferreira.voting.vote.dto;

import br.com.marcusferreira.voting.vote.VoteOption;
import com.fasterxml.jackson.annotation.JsonProperty;

public record VoteResponse(
    @JsonProperty("pautaId") Long agendaId,
    @JsonProperty("associadoId") String memberId,
    @JsonProperty("voto") VoteOption vote
) {
}
