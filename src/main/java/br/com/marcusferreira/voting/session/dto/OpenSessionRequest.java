package br.com.marcusferreira.voting.session.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenSessionRequest(@JsonProperty("duracaoSegundos") Long durationSeconds) {
}
