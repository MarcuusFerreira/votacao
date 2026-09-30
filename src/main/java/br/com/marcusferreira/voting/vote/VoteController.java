package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.agenda.AgendaService;
import br.com.marcusferreira.voting.screen.FormScreen;
import br.com.marcusferreira.voting.screen.VotingScreens;
import br.com.marcusferreira.voting.session.VotingSessionService;
import br.com.marcusferreira.voting.vote.dto.CastVoteRequest;
import br.com.marcusferreira.voting.vote.dto.VoteFormRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/formulario")
    public FormScreen voteForm(@PathVariable Long agendaId, @Valid @RequestBody VoteFormRequest request) {
        votingSessionService.getOpenSessionOrThrow(agendaId);
        return screens.voteForm(agendaService.findById(agendaId), request.vote());
    }

    @PostMapping
    public ResponseEntity<FormScreen> cast(@PathVariable Long agendaId,
                                           @Valid @RequestBody CastVoteRequest request) {
        voteService.cast(agendaId, request.memberId(), request.cpf(), request.vote());
        return ResponseEntity.status(HttpStatus.CREATED).body(screens.voteRegistered(agendaId, request.vote()));
    }
}
