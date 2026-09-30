package br.com.marcusferreira.voting.common.exception;

public class AgendaNotFoundException extends RuntimeException {
    public AgendaNotFoundException(Long id) {
        super("Pauta não encontrada: " + id);
    }
}
