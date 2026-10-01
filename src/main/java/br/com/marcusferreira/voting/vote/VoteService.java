package br.com.marcusferreira.voting.vote;

import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.member.MemberEligibilityClient;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteService.class);

    private final VoteJdbcRepository voteRepository;
    private final VotingSessionService votingSessionService;
    private final MemberEligibilityClient memberEligibilityClient;
    private final VotingProperties properties;

    public VoteService(VoteJdbcRepository voteRepository, VotingSessionService votingSessionService,
                       MemberEligibilityClient memberEligibilityClient, VotingProperties properties) {
        this.voteRepository = voteRepository;
        this.votingSessionService = votingSessionService;
        this.memberEligibilityClient = memberEligibilityClient;
        this.properties = properties;
    }

    public void cast(Long agendaId, String memberId, String cpf, VoteOption vote) {
        try {
            if (properties.member().verificationEnabled()) {
                // Checked first so a closed session or a repeated vote does not cost a call to the
                // external service.
                VotingSession session = votingSessionService.getOpenSessionOrThrow(agendaId);
                voteRepository.ensureNotVoted(session.getId(), memberId, cpf);
                memberEligibilityClient.checkEligibility(cpf);
            }
            if (!voteRepository.insertIntoOpenSession(agendaId, memberId, cpf, vote)) {
                // Nothing inserted: find out whether the session is missing or closed.
                votingSessionService.getOpenSessionOrThrow(agendaId);
                throw new SessionClosedException(agendaId);
            }
            log.debug("Vote registered: agendaId={} memberId={} vote={}", agendaId, memberId, vote);
        } catch (RuntimeException e) {
            log.warn("Vote rejected: agendaId={} memberId={} reason={}", agendaId, memberId, e.getMessage());
            throw e;
        }
    }

    public VotingResult tally(Long agendaId) {
        VotingSession session = votingSessionService.getSessionOrThrow(agendaId);
        VotingResult result = voteRepository.count(session.getId());
        log.info("Votes tallied: agendaId={} sessionId={} yes={} no={}",
            agendaId, session.getId(), result.yesVotes(), result.noVotes());
        return result;
    }
}
