package br.com.marcusferreira.voting.screen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SelectionScreen(
    @JsonProperty("tipo") String type,
    @JsonProperty("titulo") String title,
    @JsonProperty("itens") List<SelectionItem> items
) implements Screen {

    public SelectionScreen(String title, List<SelectionItem> items) {
        this("SELECAO", title, items);
    }
}
