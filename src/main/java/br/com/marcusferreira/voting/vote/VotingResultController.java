package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import br.com.marcusferreira.voting.screen.FormScreen;
import br.com.marcusferreira.voting.screen.VotingScreens;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{agendaId}/resultado")
public class VotingResultController {

    private final VoteService voteService;
    private final VotingSessionService votingSessionService;
    private final VotingScreens screens;

    public VotingResultController(VoteService voteService, VotingSessionService votingSessionService, VotingScreens screens) {
        this.voteService = voteService;
        this.votingSessionService = votingSessionService;
        this.screens = screens;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public FormScreen result(@PathVariable Long agendaId) {
        VotingSession session = votingSessionService.findSession(agendaId)
            .orElseThrow(() -> new SessionNotFoundException(agendaId));
        if (votingSessionService.isOpen(session)) {
            return screens.resultInProgress(agendaId, session.getClosesAt());
        }
        return screens.result(voteService.tally(agendaId));
    }
}
