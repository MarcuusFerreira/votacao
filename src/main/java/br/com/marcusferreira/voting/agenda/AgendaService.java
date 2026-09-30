package br.com.marcusferreira.voting.agenda;

import br.com.marcusferreira.voting.common.exception.AgendaNotFoundException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AgendaService {

    private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

    private final AgendaRepository repository;
    private final Clock clock;

    public AgendaService(AgendaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Agenda create(String title, String description) {
        Agenda agenda = repository.save(new Agenda(title, description, clock.instant()));
        log.info("Agenda created: id={} title={}", agenda.getId(), agenda.getTitle());
        return agenda;
    }

    public Agenda findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new AgendaNotFoundException(id));
    }

    public Page<Agenda> findPage(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }
}
