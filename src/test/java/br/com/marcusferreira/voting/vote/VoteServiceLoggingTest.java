package br.com.marcusferreira.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.MemberNotEligibleException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import br.com.marcusferreira.voting.member.MemberEligibilityClient;
import br.com.marcusferreira.voting.session.VotingSession;
import br.com.marcusferreira.voting.session.VotingSessionService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class VoteServiceLoggingTest {

    @Mock
    VoteJdbcRepository voteRepository;

    @Mock
    VotingSessionService votingSessionService;

    @Mock
    MemberEligibilityClient memberEligibilityClient;

    ListAppender<ILoggingEvent> appender;
    Logger logger;
    Level originalLevel;

    VoteService service;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(VoteService.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        service = new VoteService(voteRepository, votingSessionService, memberEligibilityClient,
            new VotingProperties("http://localhost:8080", new VotingProperties.Session(Duration.ofSeconds(60)),
                new VotingProperties.Member("http://example.com", false)));
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    @Test
    void registeredVoteIsLoggedAtDebugToKeepTheHotPathQuiet() {
        when(voteRepository.insertIntoOpenSession(1L, "associado-1", "12345678900", VoteOption.YES)).thenReturn(true);

        service.cast(1L, "associado-1", "12345678900", VoteOption.YES);

        assertThat(appender.list)
            .filteredOn(event -> event.getFormattedMessage().contains("Vote registered"))
            .extracting(ILoggingEvent::getLevel)
            .containsExactly(Level.DEBUG);
    }

    @Test
    void rejectedVoteIsStillLoggedAtWarn() {
        when(voteRepository.insertIntoOpenSession(1L, "associado-1", "12345678900", VoteOption.YES)).thenReturn(false);
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenThrow(new SessionNotFoundException(1L));

        assertThatThrownBy(() -> service.cast(1L, "associado-1", "12345678900", VoteOption.YES))
            .isInstanceOf(SessionNotFoundException.class);

        assertThat(appender.list)
            .filteredOn(event -> event.getFormattedMessage().contains("Vote rejected"))
            .extracting(ILoggingEvent::getLevel)
            .containsExactly(Level.WARN);
    }

    @Test
    void rejectedVoteLogDoesNotExposeTheCpf() {
        VoteService verifying = new VoteService(voteRepository, votingSessionService, memberEligibilityClient,
            new VotingProperties("http://localhost:8080", new VotingProperties.Session(Duration.ofSeconds(60)),
                new VotingProperties.Member("http://example.com", true)));
        when(votingSessionService.getOpenSessionOrThrow(1L)).thenReturn(new VotingSession(1L, Instant.now(), Duration.ofSeconds(60)));
        doThrow(new MemberNotEligibleException("12345678900")).when(memberEligibilityClient).checkEligibility("12345678900");

        assertThatThrownBy(() -> verifying.cast(1L, "associado-1", "12345678900", VoteOption.YES))
            .isInstanceOf(MemberNotEligibleException.class);

        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
            .isNotEmpty()
            .noneMatch(message -> message.contains("12345678900"));
    }
}
