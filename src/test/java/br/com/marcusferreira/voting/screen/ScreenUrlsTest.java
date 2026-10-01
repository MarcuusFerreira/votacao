package br.com.marcusferreira.voting.screen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScreenUrlsTest {

    @Test
    void buildsAbsoluteUrlsOnTheConfiguredBase() {
        ScreenUrls urls = new ScreenUrls("http://10.0.2.2:8080");

        assertThat(urls.agenda(4L)).isEqualTo("http://10.0.2.2:8080/api/v1/pautas/4");
        assertThat(urls.openSession(4L)).isEqualTo("http://10.0.2.2:8080/api/v1/pautas/4/sessoes");
        assertThat(urls.votes(4L)).isEqualTo("http://10.0.2.2:8080/api/v1/pautas/4/votos");
        assertThat(urls.result(4L)).isEqualTo("http://10.0.2.2:8080/api/v1/pautas/4/resultado");
        assertThat(urls.agendasPage(1, 20)).isEqualTo("http://10.0.2.2:8080/api/v1/pautas/lista?pagina=1&tamanho=20");
    }

    @Test
    void acceptsBaseWithTrailingSlashOrPathPrefix() {
        assertThat(new ScreenUrls("https://votos.example.com/").agenda(1L))
            .isEqualTo("https://votos.example.com/api/v1/pautas/1");
        assertThat(new ScreenUrls("https://example.com/votos").agenda(1L))
            .isEqualTo("https://example.com/votos/api/v1/pautas/1");
    }
}
