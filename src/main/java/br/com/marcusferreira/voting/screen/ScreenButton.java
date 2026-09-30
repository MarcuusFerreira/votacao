package br.com.marcusferreira.voting.screen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScreenButton(@JsonProperty("texto") String text, String url, Map<String, Object> body) {
}
