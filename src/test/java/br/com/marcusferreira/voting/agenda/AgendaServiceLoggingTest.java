package br.com.marcusferreira.voting.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class AgendaServiceLoggingTest {

    @Mock
    AgendaRepository repository;

    ListAppender<ILoggingEvent> appender;
    Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AgendaService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void createLogsAtInfoLevel() {
        AgendaService service = new AgendaService(repository, Clock.systemUTC());
        when(repository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create("Pauta logada", null);

        assertThat(appender.list).anyMatch(event ->
            event.getLevel() == Level.INFO && event.getFormattedMessage().contains("Agenda created"));
    }
}
