package br.com.marcusferreira.voting.screen;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FormItem(
    @JsonProperty("tipo") String type,
    @JsonProperty("texto") String text
) {

    public static FormItem text(String text) {
        return new FormItem("TEXTO", text);
    }
}
