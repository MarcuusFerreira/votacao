package br.com.marcusferreira.voting.screen;

import static br.com.marcusferreira.voting.MobileAppSimulator.inputIds;
import static br.com.marcusferreira.voting.MobileAppSimulator.items;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.MobileAppSimulator;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * The whole voting journey driven only by screens, as the mobile app would: no URL or body is
 * built by the client besides the values typed into the inputs.
 */
class MobileFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createAgendaOpenSessionVoteAndSeeResultOnlyThroughScreens() {
        MobileAppSimulator app = new MobileAppSimulator(restTemplate);

        Map<String, Object> screen = app.open("/api/v1/pautas");
        screen = app.select(screen, "Nova pauta");
        assertThat(inputIds(screen)).containsExactly("titulo", "descricao");

        screen = app.pressOk(screen, Map.of("titulo", "Pauta pelo app", "descricao", "Fluxo completo"));
        assertThat(app.lastStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(screen.get("titulo")).isEqualTo("Pauta pelo app");
        assertThat(inputIds(screen)).containsExactly("duracaoSegundos");

        screen = app.pressOk(screen, Map.of("duracaoSegundos", 60));
        assertThat(app.lastStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(items(screen)).extracting(item -> item.get("texto")).containsExactly("Sim", "Não");

        screen = app.select(screen, "Sim");
        assertThat(inputIds(screen)).containsExactly("associadoId", "cpf");

        screen = app.pressOk(screen, Map.of("associadoId", "associado-app", "cpf", "12345678900"));
        assertThat(app.lastStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(items(screen).getFirst().get("texto")).asString().contains("registrado");

        clock.advance(Duration.ofSeconds(61));
        screen = app.pressOk(screen, Map.of());
        assertThat(screen.get("titulo")).isEqualTo("Resultado");
        assertThat(items(screen).getFirst().get("texto")).asString().contains("Sim: 1 / Não: 0");

        screen = app.pressOk(screen, Map.of());
        assertThat(screen.get("tipo")).isEqualTo("SELECAO");
        assertThat(items(screen)).extracting(item -> item.get("texto")).contains("Pauta pelo app");
    }

    @Test
    void openAgendaWithoutSessionAndCancelBackToTheList() {
        MobileAppSimulator app = new MobileAppSimulator(restTemplate);

        Map<String, Object> form = app.select(app.open("/api/v1/pautas"), "Nova pauta");
        Map<String, Object> list = app.pressCancel(form);

        assertThat(app.lastStatus()).isEqualTo(HttpStatus.OK);
        assertThat(list.get("tipo")).isEqualTo("SELECAO");
    }
}
