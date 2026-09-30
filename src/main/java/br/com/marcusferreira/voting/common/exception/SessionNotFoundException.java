package br.com.marcusferreira.voting.common.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(Long agendaId) {
        super("Nenhuma sessão de votação encontrada para a pauta: " + agendaId);
    }
}
