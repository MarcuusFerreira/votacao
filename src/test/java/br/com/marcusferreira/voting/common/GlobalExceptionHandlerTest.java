package br.com.marcusferreira.voting.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.marcusferreira.voting.common.exception.AgendaNotFoundException;
import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import br.com.marcusferreira.voting.common.exception.InvalidCpfException;
import br.com.marcusferreira.voting.common.exception.MemberNotEligibleException;
import br.com.marcusferreira.voting.common.exception.SessionAlreadyOpenException;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ThrowingController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @RestController
    static class ThrowingController {
        @GetMapping("/test/agenda-not-found")
        void agendaNotFound() { throw new AgendaNotFoundException(1L); }

        @GetMapping("/test/session-not-found")
        void sessionNotFound() { throw new SessionNotFoundException(1L); }

        @GetMapping("/test/session-already-open")
        void sessionAlreadyOpen() { throw new SessionAlreadyOpenException(1L); }

        @GetMapping("/test/session-closed")
        void sessionClosed() { throw new SessionClosedException(1L); }

        @GetMapping("/test/duplicate-vote")
        void duplicateVote() { throw new DuplicateVoteException(1L, "abc"); }

        @GetMapping("/test/invalid-cpf")
        void invalidCpf() { throw new InvalidCpfException("11111111111"); }

        @GetMapping("/test/member-not-eligible")
        void memberNotEligible() { throw new MemberNotEligibleException("11111111111"); }
    }

    @Test
    void agendaNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/test/agenda-not-found")).andExpect(status().isNotFound());
    }

    @Test
    void sessionNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/test/session-not-found")).andExpect(status().isNotFound());
    }

    @Test
    void sessionAlreadyOpenReturns409() throws Exception {
        mockMvc.perform(get("/test/session-already-open")).andExpect(status().isConflict());
    }

    @Test
    void sessionClosedReturns409() throws Exception {
        mockMvc.perform(get("/test/session-closed")).andExpect(status().isConflict());
    }

    @Test
    void duplicateVoteReturns409() throws Exception {
        mockMvc.perform(get("/test/duplicate-vote")).andExpect(status().isConflict());
    }

    @Test
    void invalidCpfReturns404() throws Exception {
        mockMvc.perform(get("/test/invalid-cpf")).andExpect(status().isNotFound());
    }

    @Test
    void memberNotEligibleReturns403() throws Exception {
        mockMvc.perform(get("/test/member-not-eligible")).andExpect(status().isForbidden());
    }
}
