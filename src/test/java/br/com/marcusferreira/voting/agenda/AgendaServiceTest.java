package br.com.marcusferreira.voting.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.marcusferreira.voting.common.exception.AgendaNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    @Mock
    AgendaRepository repository;

    @Test
    void createSavesTitleAndDescription() {
        AgendaService service = new AgendaService(repository);
        when(repository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        Agenda agenda = service.create("Reforma do estatuto", "Detalhes");

        assertThat(agenda.getTitle()).isEqualTo("Reforma do estatuto");
        assertThat(agenda.getDescription()).isEqualTo("Detalhes");
        verify(repository).save(any(Agenda.class));
    }

    @Test
    void findByIdThrowsWhenMissing() {
        AgendaService service = new AgendaService(repository);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
            .isInstanceOf(AgendaNotFoundException.class);
    }
}
