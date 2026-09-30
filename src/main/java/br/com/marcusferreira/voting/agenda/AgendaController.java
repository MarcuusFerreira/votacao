package br.com.marcusferreira.voting.agenda;

import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import br.com.marcusferreira.voting.screen.FormItem;
import br.com.marcusferreira.voting.screen.FormScreen;
import br.com.marcusferreira.voting.screen.Screen;
import br.com.marcusferreira.voting.screen.ScreenButton;
import br.com.marcusferreira.voting.screen.ScreenUrls;
import br.com.marcusferreira.voting.screen.SelectionItem;
import br.com.marcusferreira.voting.screen.SelectionScreen;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas")
public class AgendaController {

    private final AgendaService agendaService;
    private final VotingSessionService votingSessionService;
    private final ScreenUrls urls;

    public AgendaController(AgendaService agendaService, VotingSessionService votingSessionService, ScreenUrls urls) {
        this.agendaService = agendaService;
        this.votingSessionService = votingSessionService;
        this.urls = urls;
    }

    @PostMapping
    public ResponseEntity<AgendaResponse> create(@Valid @RequestBody CreateAgendaRequest request) {
        Agenda agenda = agendaService.create(request.title(), request.description());
        return ResponseEntity.created(URI.create(urls.agenda(agenda.getId())))
            .body(AgendaResponse.from(agenda));
    }

    @GetMapping
    public SelectionScreen list(
            @RequestParam(name = "pagina", defaultValue = "0") @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int page,
            @RequestParam(name = "tamanho", defaultValue = "20") @Min(value = 1, message = "tamanho deve ser no mínimo 1")
            @Max(value = 100, message = "tamanho deve ser no máximo 100") int size) {
        Page<Agenda> agendas = agendaService.findPage(page, size);
        List<SelectionItem> items = new ArrayList<>();
        agendas.forEach(agenda -> items.add(new SelectionItem(agenda.getTitle(), urls.agenda(agenda.getId()))));
        if (agendas.hasNext()) {
            items.add(new SelectionItem("Próxima página", urls.agendasPage(page + 1, size)));
        }
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
                new ScreenButton("Abrir sessão", urls.openSession(id), null),
                null);
        }

        VotingSession session = currentSession.get();
        if (votingSessionService.isOpen(session)) {
            String voteUrl = urls.votes(id);
            return new SelectionScreen(agenda.getTitle(), List.of(
                new SelectionItem("Sim", voteUrl, Map.of("voto", "SIM")),
                new SelectionItem("Não", voteUrl, Map.of("voto", "NAO"))
            ));
        }

        return new FormScreen(
            agenda.getTitle(),
            List.of(FormItem.text("Sessão encerrada. Consulte o resultado.")),
            new ScreenButton("Ver resultado", urls.result(id), null),
            null);
    }
}
