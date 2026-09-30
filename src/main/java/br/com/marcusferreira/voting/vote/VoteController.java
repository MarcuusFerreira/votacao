package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.screen.FormScreen;
import br.com.marcusferreira.voting.screen.VotingScreens;
import br.com.marcusferreira.voting.session.VotingSessionService;
import br.com.marcusferreira.voting.vote.dto.CastVoteRequest;
import br.com.marcusferreira.voting.vote.dto.VoteFormRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Votos")
@RestController
@RequestMapping("/api/v1/pautas/{agendaId}/votos")
public class VoteController {

    private final VoteService voteService;
    private final AgendaService agendaService;
    private final VotingSessionService votingSessionService;
    private final VotingScreens screens;

    public VoteController(VoteService voteService, AgendaService agendaService,
                          VotingSessionService votingSessionService, VotingScreens screens) {
        this.voteService = voteService;
        this.agendaService = agendaService;
        this.votingSessionService = votingSessionService;
        this.screens = screens;
    }

    // Reached from the SELECAO Sim/Não: collects the member's identification for the chosen option.
    @Operation(summary = "Formulário de voto", description = "Recebe a opção escolhida na tela de votação e pede associadoId e cpf.")
    @PostMapping("/formulario")
    public FormScreen voteForm(@PathVariable Long agendaId, @Valid @RequestBody VoteFormRequest request) {
        votingSessionService.getOpenSessionOrThrow(agendaId);
        return screens.voteForm(agendaService.findById(agendaId), request.vote());
    }

    @Operation(summary = "Registra um voto", description = "Um voto por associado e por CPF em cada pauta. Responde 201 com a tela de confirmação.")
    @PostMapping
    public ResponseEntity<FormScreen> cast(@PathVariable Long agendaId,
                                           @Valid @RequestBody CastVoteRequest request) {
        voteService.cast(agendaId, request.memberId(), request.cpf(), request.vote());
        return ResponseEntity.status(HttpStatus.CREATED).body(screens.voteRegistered(agendaId, request.vote()));
    }
}
