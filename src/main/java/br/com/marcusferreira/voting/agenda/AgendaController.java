package br.com.marcusferreira.voting.agenda;

import br.com.marcusferreira.voting.agenda.dto.CreateAgendaRequest;
import br.com.marcusferreira.voting.screen.FormScreen;
import br.com.marcusferreira.voting.screen.Screen;
import br.com.marcusferreira.voting.screen.ScreenUrls;
import br.com.marcusferreira.voting.screen.SelectionScreen;
import br.com.marcusferreira.voting.screen.VotingScreens;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// The app POSTs to every URL embedded in a screen (Annex 1), so navigation endpoints accept both
// GET (direct access, e.g. the first screen) and POST.
@Tag(name = "Pautas")
@RestController
@RequestMapping("/api/v1/pautas")
public class AgendaController {

    private final AgendaService agendaService;
    private final VotingSessionService votingSessionService;
    private final VotingScreens screens;
    private final ScreenUrls urls;

    public AgendaController(AgendaService agendaService, VotingSessionService votingSessionService,
                            VotingScreens screens, ScreenUrls urls) {
        this.agendaService = agendaService;
        this.votingSessionService = votingSessionService;
        this.screens = screens;
        this.urls = urls;
    }

    @Operation(summary = "Cadastra uma pauta", description = "Responde 201 com a URL da pauta no header Location e a tela de abertura de sessão.")
    @PostMapping
    public ResponseEntity<FormScreen> create(@Valid @RequestBody CreateAgendaRequest request) {
        Agenda agenda = agendaService.create(request.title(), request.description());
        return ResponseEntity.created(URI.create(urls.agenda(agenda.getId())))
            .body(screens.openSessionForm(agenda));
    }

    // POST with "pagina" is navigation (e.g. "Próxima página"); a POST without it creates an agenda.
    @Operation(summary = "Página da lista de pautas", description = "Tela SELECAO; acessível por POST porque é o destino do item \"Próxima página\" e do botão \"Voltar\".")
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, params = "pagina")
    public SelectionScreen listPage(
            @RequestParam(name = "pagina") @Min(value = 0, message = "pagina deve ser maior ou igual a zero") int page,
            @RequestParam(name = "tamanho", defaultValue = "20") @Min(value = 1, message = "tamanho deve ser no mínimo 1")
            @Max(value = 100, message = "tamanho deve ser no máximo 100") int size) {
        return screens.agendaList(agendaService.findPage(page, size), page, size);
    }

    @Operation(summary = "Lista as pautas", description = "Tela SELECAO de entrada do app: \"Nova pauta\", pautas mais recentes primeiro e \"Próxima página\".")
    @GetMapping
    public SelectionScreen list(
            @RequestParam(name = "tamanho", defaultValue = "20") @Min(value = 1, message = "tamanho deve ser no mínimo 1")
            @Max(value = 100, message = "tamanho deve ser no máximo 100") int size) {
        return screens.agendaList(agendaService.findPage(0, size), 0, size);
    }

    @Operation(summary = "Formulário de nova pauta", description = "Tela FORMULARIO com os campos titulo e descricao.")
    @RequestMapping(path = "/formulario", method = {RequestMethod.GET, RequestMethod.POST})
    public FormScreen newAgendaForm() {
        return screens.newAgendaForm();
    }

    @Operation(summary = "Tela da pauta", description = "Sem sessão: formulário de abertura; sessão aberta: SELECAO Sim/Não; encerrada: acesso ao resultado.")
    @RequestMapping(path = "/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public Screen detail(@PathVariable Long id) {
        Agenda agenda = agendaService.findById(id);
        Optional<VotingSession> session = votingSessionService.findSession(id);
        if (session.isEmpty()) {
            return screens.openSessionForm(agenda);
        }
        return votingSessionService.isOpen(session.get())
            ? screens.voteOptions(agenda)
            : screens.sessionClosed(agenda);
    }
}
