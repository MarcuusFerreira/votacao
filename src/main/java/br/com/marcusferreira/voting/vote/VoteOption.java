package br.com.marcusferreira.voting.vote;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum VoteOption {
    @JsonProperty("SIM") YES,
    @JsonProperty("NAO") NO
}
