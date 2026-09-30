package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import br.com.marcusferreira.voting.member.MemberEligibilityClient;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
        return new VotingProperties("http://localhost:8080", 
            new VotingProperties.Session(Duration.ofSeconds(60)),
            new VotingProperties.Member("http://example.com", verificationEnabled));
    }

    @Test
    void castChecksSessionAndEligibilityBeforeInsertingWhenVerificationEnabled() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(true));
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenReturn(new VotingSession(1L, Instant.now(), Duration.ofSeconds(60)));
        when(voteRepository.insertIntoOpenSession(1L, "associado-1", VoteOption.YES)).thenReturn(true);

        service.cast(1L, "associado-1", "12345678900", VoteOption.YES);

        InOrder inOrder = inOrder(votingSessionService, memberEligibilityClient, voteRepository);
        inOrder.verify(votingSessionService).getOpenSessionOrThrow(1L);
        inOrder.verify(memberEligibilityClient).checkEligibility("12345678900");
        inOrder.verify(voteRepository).insertIntoOpenSession(1L, "associado-1", VoteOption.YES);
    }

    @Test
    void castSkipsEligibilityCheckWhenSessionClosed() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(true));
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenThrow(new SessionClosedException(1L));

        assertThatThrownBy(() -> service.cast(1L, "associado-1", "12345678900", VoteOption.YES))
            .isInstanceOf(SessionClosedException.class);
        verify(memberEligibilityClient, never()).checkEligibility(any());
        verify(voteRepository, never()).insertIntoOpenSession(any(), any(), any());
    }

    @Test
    void castInsertsWithSingleStatementWhenVerificationDisabled() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(false));
        when(voteRepository.insertIntoOpenSession(1L, "associado-1", VoteOption.YES)).thenReturn(true);

        service.cast(1L, "associado-1", "12345678900", VoteOption.YES);

        verify(memberEligibilityClient, never()).checkEligibility(any());
        verifyNoInteractions(votingSessionService);
    }

    @Test
    void castReportsWhySessionRejectedTheVoteWhenNothingWasInserted() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(false));
        when(voteRepository.insertIntoOpenSession(1L, "associado-1", VoteOption.YES)).thenReturn(false);
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenThrow(new SessionNotFoundException(1L));

        assertThatThrownBy(() -> service.cast(1L, "associado-1", "12345678900", VoteOption.YES))
            .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void castTreatsSessionAsClosedWhenNothingWasInsertedButSessionLooksOpen() {
        VoteService service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient, properties(false));
        when(voteRepository.insertIntoOpenSession(1L, "associado-1", VoteOption.YES)).thenReturn(false);
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenReturn(new VotingSession(1L, Instant.now(), Duration.ofSeconds(60)));

        assertThatThrownBy(() -> service.cast(1L, "associado-1", "12345678900", VoteOption.YES))
            .isInstanceOf(SessionClosedException.class);
    }
}
