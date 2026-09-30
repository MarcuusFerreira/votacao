package br.com.marcusferreira.voting.common.exception;

public class SessionAlreadyOpenException extends RuntimeException {
    public SessionAlreadyOpenException(Long agendaId) {
        super("Já existe uma sessão de votação aberta para a pauta: " + agendaId);
    }

    public SessionAlreadyOpenException(Long agendaId, Throwable cause) {
        super("Já existe uma sessão de votação para a pauta (cada pauta admite uma única sessão): " + agendaId, cause);
    }
}
