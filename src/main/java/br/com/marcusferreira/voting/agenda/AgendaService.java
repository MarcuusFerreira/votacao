package br.com.marcusferreira.voting.agenda;

import br.com.marcusferreira.voting.common.exception.AgendaNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgendaService {

    private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

    private final AgendaRepository repository;

    public AgendaService(AgendaRepository repository) {
        this.repository = repository;
    }

    public Agenda create(String title, String description) {
        Agenda agenda = repository.save(new Agenda(title, description));
        log.info("Agenda created: id={} title={}", agenda.getId(), agenda.getTitle());
        return agenda;
    }

    public Agenda findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new AgendaNotFoundException(id));
    }

    public List<Agenda> findAll() {
        return repository.findAll();
    }
}
