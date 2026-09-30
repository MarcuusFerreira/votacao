package br.com.marcusferreira.voting.agenda;

import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import br.com.marcusferreira.voting.screen.FormItem;
import br.com.marcusferreira.voting.screen.FormScreen;
import br.com.marcusferreira.voting.screen.Screen;
import br.com.marcusferreira.voting.screen.ScreenButton;
import br.com.marcusferreira.voting.screen.SelectionItem;
import br.com.marcusferreira.voting.screen.SelectionScreen;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas")
public class AgendaController {

    private final AgendaService agendaService;
    private final VotingSessionService votingSessionService;

    public AgendaController(AgendaService agendaService, VotingSessionService votingSessionService) {
        this.agendaService = agendaService;
        this.votingSessionService = votingSessionService;
    }

    @PostMapping
    public ResponseEntity<AgendaResponse> create(@Valid @RequestBody CreateAgendaRequest request) {
        Agenda agenda = agendaService.create(request.title(), request.description());
        return ResponseEntity.created(URI.create("/api/v1/pautas/" + agenda.getId()))
            .body(AgendaResponse.from(agenda));
    }

    @GetMapping
    public SelectionScreen list() {
        List<SelectionItem> items = agendaService.findAll().stream()
            .map(agenda -> new SelectionItem(agenda.getTitle(), "/api/v1/pautas/" + agenda.getId()))
            .toList();
        return new SelectionScreen("Pautas", items);
    }

    @GetMapping("/{id}")
    public Screen detail(@PathVariable Long id) {
        Agenda agenda = agendaService.findById(id);
        var currentSession = votingSessionService.findSession(id);

        if (currentSession.isEmpty()) {
            return new FormScreen(
                agenda.getTitle(),
                List.of(FormItem.text(agenda.getDescription() != null ? agenda.getDescription() : "")),
                new ScreenButton("Abrir sessão", "/api/v1/pautas/" + id + "/sessoes", null),
                null);
        }

        VotingSession session = currentSession.get();
        if (votingSessionService.isOpen(session)) {
            String voteUrl = "/api/v1/pautas/" + id + "/votos";
            return new SelectionScreen(agenda.getTitle(), List.of(
                new SelectionItem("Sim", voteUrl, Map.of("voto", "SIM")),
                new SelectionItem("Não", voteUrl, Map.of("voto", "NAO"))
            ));
        }

        return new FormScreen(
            agenda.getTitle(),
            List.of(FormItem.text("Sessão encerrada. Consulte o resultado.")),
            new ScreenButton("Ver resultado", "/api/v1/pautas/" + id + "/resultado", null),
            null);
    }
}
