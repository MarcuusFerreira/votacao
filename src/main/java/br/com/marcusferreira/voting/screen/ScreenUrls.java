package br.com.marcusferreira.voting.screen;

import br.com.marcusferreira.voting.common.VotingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Single place that builds the callback URLs embedded in screens. They are absolute and based on
 * {@code voting.public-base-url}, so the same server can be reached from an emulator, a physical
 * device or a public domain just by configuration.
 */
@Component
public class ScreenUrls {

    private static final String AGENDAS = "/api/v1/pautas";

    private final String publicBaseUrl;

    @Autowired
    public ScreenUrls(VotingProperties properties) {
        this(properties.publicBaseUrl());
    }

    public ScreenUrls(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
            ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
            : publicBaseUrl;
    }

    public String agendas() {
        return build(AGENDAS);
    }

    public String agendasPage(int page, int size) {
        return UriComponentsBuilder.fromUriString(build(AGENDAS + "/lista"))
            .queryParam("pagina", page)
            .queryParam("tamanho", size)
            .toUriString();
    }

    public String newAgendaForm() {
        return build(AGENDAS + "/formulario");
    }

    public String agenda(Long agendaId) {
        return build(AGENDAS + "/{id}", agendaId);
    }

    public String openSession(Long agendaId) {
        return build(AGENDAS + "/{id}/sessoes", agendaId);
    }

    public String votes(Long agendaId) {
        return build(AGENDAS + "/{id}/votos", agendaId);
    }

    public String voteForm(Long agendaId) {
        return build(AGENDAS + "/{id}/votos/formulario", agendaId);
    }

    public String result(Long agendaId) {
        return build(AGENDAS + "/{id}/resultado", agendaId);
    }

    private String build(String path, Object... variables) {
        return UriComponentsBuilder.fromUriString(publicBaseUrl).path(path).buildAndExpand(variables).toUriString();
    }
}
