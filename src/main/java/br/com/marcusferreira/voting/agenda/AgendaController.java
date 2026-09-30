package br.com.marcusferreira.voting.agenda;

import br.com.marcusferreira.voting.agenda.dto.AgendaResponse;
import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import br.com.marcusferreira.voting.screen.SelectionItem;
import br.com.marcusferreira.voting.screen.SelectionScreen;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
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
}
