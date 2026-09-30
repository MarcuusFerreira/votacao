package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import br.com.marcusferreira.voting.screen.FormItem;
import br.com.marcusferreira.voting.screen.FormScreen;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{agendaId}/resultado")
public class VotingResultController {

    private final VoteService voteService;
    private final VotingSessionService votingSessionService;

    public VotingResultController(VoteService voteService, VotingSessionService votingSessionService) {
        this.voteService = voteService;
        this.votingSessionService = votingSessionService;
    }

    @GetMapping
    public FormScreen result(@PathVariable Long agendaId) {
        VotingSession session = votingSessionService.findSession(agendaId)
            .orElseThrow(() -> new SessionNotFoundException(agendaId));

        if (votingSessionService.isOpen(session)) {
            return new FormScreen("Resultado",
                List.of(FormItem.text("Sessão em andamento até " + session.getClosesAt())),
                null, null);
        }

        VotingResult result = voteService.tally(agendaId);
        String text = "Sim: %d / Não: %d — Vencedor: %s"
            .formatted(result.yesVotes(), result.noVotes(), result.winner().label());
        return new FormScreen("Resultado", List.of(FormItem.text(text)), null, null);
    }
}
