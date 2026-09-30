package br.com.marcusferreira.voting.screen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormScreen(
    @JsonProperty("tipo") String type,
    @JsonProperty("titulo") String title,
    @JsonProperty("itens") List<FormItem> items,
    @JsonProperty("botaoOk") ScreenButton okButton,
    @JsonProperty("botaoCancelar") ScreenButton cancelButton
) implements Screen {

    public FormScreen(String title, List<FormItem> items, ScreenButton okButton, ScreenButton cancelButton) {
        this("FORMULARIO", title, items, okButton, cancelButton);
    }
}
