package br.com.marcusferreira.voting.screen;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScreenSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesFormScreenWithPortugueseContract() throws Exception {
        FormScreen screen = new FormScreen(
            "Minha Pauta",
            List.of(FormItem.text("Descrição da pauta")),
            new ScreenButton("Abrir sessão", "/api/v1/pautas/1/sessoes", null),
            null
        );

        String json = mapper.writeValueAsString(screen);

        assertThat(json).contains("\"tipo\":\"FORMULARIO\"");
        assertThat(json).contains("\"titulo\":\"Minha Pauta\"");
        assertThat(json).contains("\"itens\":[");
        assertThat(json).contains("\"texto\":\"Descrição da pauta\"");
        assertThat(json).contains("\"botaoOk\":{");
        assertThat(json).doesNotContain("botaoCancelar");
    }

    @Test
    void serializesSelectionScreenWithItemsAndBody() throws Exception {
        SelectionScreen screen = new SelectionScreen(
            "Vote",
            List.of(
                new SelectionItem("Sim", "/api/v1/pautas/1/votos", Map.of("voto", "SIM")),
                new SelectionItem("Não", "/api/v1/pautas/1/votos", Map.of("voto", "NAO"))
            )
        );

        String json = mapper.writeValueAsString(screen);

        assertThat(json).contains("\"tipo\":\"SELECAO\"");
        assertThat(json).contains("\"voto\":\"SIM\"");
    }

    @Test
    void selectionItemWithoutBodyOmitsField() throws Exception {
        SelectionItem item = new SelectionItem("Opção 1", "/api/v1/pautas/1");

        String json = mapper.writeValueAsString(item);

        assertThat(json).doesNotContain("body");
    }
}
