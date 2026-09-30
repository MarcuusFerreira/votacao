package br.com.marcusferreira.voting.screen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Item of a FORMULARIO screen: a read-only text or an input whose value the app sends back,
 * keyed by {@code id}, when a button is pressed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"tipo", "texto", "id", "titulo", "valor"})
public record FormItem(
    @JsonProperty("tipo") String type,
    @JsonProperty("texto") String text,
    @JsonProperty("id") String id,
    @JsonProperty("titulo") String title,
    @JsonProperty("valor") Object value
) {

    public static FormItem text(String text) {
        return new FormItem("TEXTO", text, null, null, null);
    }

    public static FormItem inputText(String id, String title, String value) {
        return new FormItem("INPUT_TEXTO", null, id, title, value);
    }

    public static FormItem inputNumber(String id, String title, Number value) {
        return new FormItem("INPUT_NUMERO", null, id, title, value);
    }
}
