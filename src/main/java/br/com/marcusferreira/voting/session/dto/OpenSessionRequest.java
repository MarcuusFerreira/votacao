package br.com.marcusferreira.voting.session.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record OpenSessionRequest(
    @JsonProperty("duracaoSegundos")
    @Positive(message = "duracaoSegundos deve ser maior que zero")
    @Max(value = OpenSessionRequest.MAX_DURATION_SECONDS, message = "duracaoSegundos deve ser no máximo 86400 (24 horas)")
    Long durationSeconds
) {

    public static final long MAX_DURATION_SECONDS = 86_400;
}
