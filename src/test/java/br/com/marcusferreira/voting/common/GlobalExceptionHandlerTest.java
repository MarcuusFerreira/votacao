package br.com.marcusferreira.voting.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.marcusferreira.voting.common.exception.AgendaNotFoundException;
import br.com.marcusferreira.voting.common.exception.CpfAlreadyUsedException;
import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import br.com.marcusferreira.voting.common.exception.InvalidCpfException;
import br.com.marcusferreira.voting.common.exception.MemberNotEligibleException;
import br.com.marcusferreira.voting.common.exception.MemberVerificationUnavailableException;
import br.com.marcusferreira.voting.common.exception.SessionAlreadyOpenException;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import br.com.marcusferreira.voting.vote.dto.CastVoteRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        void duplicateVote() { throw new DuplicateVoteException("abc"); }

        @GetMapping("/test/invalid-cpf")
        void invalidCpf() { throw new InvalidCpfException("11111111111"); }

        @GetMapping("/test/member-not-eligible")
        void memberNotEligible() { throw new MemberNotEligibleException("11111111111"); }

        @GetMapping("/test/cpf-already-used")
        void cpfAlreadyUsed() { throw new CpfAlreadyUsedException("11111111111"); }

        @GetMapping("/test/verification-unavailable")
        void verificationUnavailable() { throw new MemberVerificationUnavailableException(new RuntimeException("timeout")); }

        @GetMapping("/test/agendas/{id}")
        Long agenda(@PathVariable Long id) { return id; }

        @GetMapping("/test/unexpected")
        void unexpected() { throw new IllegalStateException("detalhe interno sensível"); }

        @PostMapping("/test/validation")
        CastVoteRequest validation(@Valid @RequestBody CastVoteRequest request) { return request; }
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

    @Test
    void validationErrorsUsePortugueseJsonFieldNames() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"associadoId\":\"\",\"cpf\":\"12345678900\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erros.associadoId").value("associadoId é obrigatório"))
            .andExpect(jsonPath("$.erros.voto").value("voto é obrigatório"));
    }

    @Test
    void voteOptionIsExposedAsSimNao() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"associadoId\":\"a1\",\"cpf\":\"12345678900\",\"voto\":\"NAO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.associadoId").value("a1"))
            .andExpect(jsonPath("$.voto").value("NAO"));
    }

    @Test
    void malformedJsonReturnsProblemDetailInPortuguese() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{\"associadoId\":"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Corpo da requisição inválido ou malformado"));
    }

    @Test
    void unknownVoteOptionReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"associadoId\":\"a1\",\"cpf\":\"12345678900\",\"voto\":\"TALVEZ\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Corpo da requisição inválido ou malformado"));
    }

    @Test
    void invalidPathParameterReturnsProblemDetailWithoutInternalMessage() throws Exception {
        mockMvc.perform(get("/test/agendas/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Parâmetro inválido: id"))
            .andExpect(content().string(not(containsString("For input string"))));
    }

    @Test
    void unknownRouteReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/test/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Recurso não encontrado"))
            .andExpect(content().string(not(containsString("static"))));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Erro interno inesperado"))
            .andExpect(content().string(not(containsString("sensível"))));
    }

    @Test
    void verificationUnavailableReturns503() throws Exception {
        mockMvc.perform(get("/test/verification-unavailable"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("Serviço de verificação de CPF indisponível. Tente novamente em instantes."));
    }

    @Test
    void cpfAlreadyUsedReturns409() throws Exception {
        mockMvc.perform(get("/test/cpf-already-used")).andExpect(status().isConflict());
    }

    @Test
    void cpfMustHaveElevenDigits() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"associadoId\":\"a1\",\"cpf\":\"123.456\",\"voto\":\"SIM\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erros.cpf").value("cpf deve conter 11 dígitos numéricos"));
    }

    @Test
    void unsupportedMethodReturnsProblemDetailInPortuguese() throws Exception {
        mockMvc.perform(delete("/test/agendas/1"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.detail").value("Método HTTP não suportado neste recurso"));
    }
}
