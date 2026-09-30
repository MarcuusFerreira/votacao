package br.com.marcusferreira.voting.common.exception;

public class DuplicateVoteException extends RuntimeException {
    public DuplicateVoteException(Long sessionId, String memberId) {
        super("Associado " + memberId + " já votou na sessão " + sessionId);
    }

    public DuplicateVoteException(Long sessionId, String memberId, Throwable cause) {
        super("Associado " + memberId + " já votou na sessão " + sessionId, cause);
    }
}
