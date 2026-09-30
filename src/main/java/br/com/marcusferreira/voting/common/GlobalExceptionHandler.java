package br.com.marcusferreira.voting.common;

import br.com.marcusferreira.voting.common.exception.AgendaNotFoundException;
import br.com.marcusferreira.voting.common.exception.DuplicateVoteException;
import br.com.marcusferreira.voting.common.exception.InvalidCpfException;
import br.com.marcusferreira.voting.common.exception.MemberNotEligibleException;
import br.com.marcusferreira.voting.common.exception.SessionAlreadyOpenException;
import br.com.marcusferreira.voting.common.exception.SessionClosedException;
import br.com.marcusferreira.voting.common.exception.SessionNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgendaNotFoundException.class)
    public ProblemDetail handleAgendaNotFound(AgendaNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ProblemDetail handleSessionNotFound(SessionNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SessionAlreadyOpenException.class)
    public ProblemDetail handleSessionAlreadyOpen(SessionAlreadyOpenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SessionClosedException.class)
    public ProblemDetail handleSessionClosed(SessionClosedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicateVoteException.class)
    public ProblemDetail handleDuplicateVote(DuplicateVoteException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCpfException.class)
    public ProblemDetail handleInvalidCpf(InvalidCpfException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MemberNotEligibleException.class)
    public ProblemDetail handleMemberNotEligible(MemberNotEligibleException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail("Erro de validação");
        Object target = ex.getBindingResult().getTarget();
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(jsonFieldName(target, fieldError.getField()), fieldError.getDefaultMessage());
        }
        problem.setProperty("erros", errors);
        return problem;
    }

    // Bean Validation reports the Java field name; the client only knows the JSON name
    // (the public contract is in Portuguese), so translate it through @JsonProperty.
    private static String jsonFieldName(Object target, String field) {
        if (target == null) {
            return field;
        }
        try {
            Field declared = target.getClass().getDeclaredField(field);
            JsonProperty jsonProperty = declared.getAnnotation(JsonProperty.class);
            return jsonProperty != null && !jsonProperty.value().isEmpty() ? jsonProperty.value() : field;
        } catch (NoSuchFieldException e) {
            return field;
        }
    }
}
