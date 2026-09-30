package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.vote.dto.CastVoteRequest;
import br.com.marcusferreira.voting.vote.dto.VoteResponse;
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

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping
    public ResponseEntity<VoteResponse> cast(@PathVariable Long agendaId,
                                             @Valid @RequestBody CastVoteRequest request) {
        voteService.cast(agendaId, request.memberId(), request.cpf(), request.vote());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new VoteResponse(agendaId, request.memberId(), request.vote()));
    }
}
