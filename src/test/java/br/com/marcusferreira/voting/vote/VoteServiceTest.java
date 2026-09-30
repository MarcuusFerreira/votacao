package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.member.MemberEligibilityClient;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    VoteJdbcRepository voteRepository;

    @Mock
    VotingSessionService votingSessionService;

    @Mock
    MemberEligibilityClient memberEligibilityClient;

    private static VotingProperties properties(boolean verificationEnabled) {
        return new VotingProperties(
            new VotingProperties.Session(Duration.ofSeconds(60)),
            new VotingProperties.Member("http://example.com", verificationEnabled));
    }

    @Test
    void castChecksEligibilityAndInsertsVote() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(true));
        VotingSession session = new VotingSession(1L, Duration.ofSeconds(60));
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenReturn(session);

        service.cast(1L, "associado-1", "12345678900", VoteOption.YES);

        verify(memberEligibilityClient).checkEligibility("12345678900");
        verify(voteRepository).insert(session.getId(), "associado-1", VoteOption.YES);
    }

    @Test
    void castPropagatesExceptionWhenSessionClosed() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(true));
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenThrow(new SessionClosedException(1L));

        assertThatThrownBy(() -> service.cast(1L, "associado-1", "12345678900", VoteOption.YES))
            .isInstanceOf(SessionClosedException.class);
        verify(memberEligibilityClient, never()).checkEligibility(any());
        verify(voteRepository, never()).insert(any(), any(), any());
    }

    @Test
    void castSkipsEligibilityCheckWhenVerificationDisabled() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(false));
        VotingSession session = new VotingSession(1L, Duration.ofSeconds(60));
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenReturn(session);

        service.cast(1L, "associado-1", "12345678900", VoteOption.YES);

        verify(memberEligibilityClient, never()).checkEligibility(any());
        verify(voteRepository).insert(session.getId(), "associado-1", VoteOption.YES);
    }
}
