package br.com.marcusferreira.voting.session;

import br.com.marcusferreira.voting.session.dto.OpenSessionRequest;
import br.com.marcusferreira.voting.session.dto.VotingSessionResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{agendaId}/sessoes")
public class VotingSessionController {

    private final VotingSessionService service;

    public VotingSessionController(VotingSessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<VotingSessionResponse> open(@PathVariable Long agendaId,
                                                      @Valid @RequestBody(required = false) OpenSessionRequest request) {
        Duration duration = (request != null && request.durationSeconds() != null)
            ? Duration.ofSeconds(request.durationSeconds())
            : null;
        VotingSession session = service.open(agendaId, duration);
        return ResponseEntity.status(HttpStatus.CREATED).body(VotingSessionResponse.from(session));
    }
}
