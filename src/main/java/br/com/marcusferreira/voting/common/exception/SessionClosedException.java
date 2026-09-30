package br.com.marcusferreira.voting.common.exception;

public class SessionClosedException extends RuntimeException {
    public SessionClosedException(Long agendaId) {
        super("A sessão de votação da pauta " + agendaId + " está fechada");
    }
}
