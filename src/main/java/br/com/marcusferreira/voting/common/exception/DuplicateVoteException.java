package br.com.marcusferreira.voting.common.exception;

public class DuplicateVoteException extends RuntimeException {
    public DuplicateVoteException(String memberId) {
        super("O associado " + memberId + " já votou nesta pauta");
    }

    public DuplicateVoteException(String memberId, Throwable cause) {
        super("O associado " + memberId + " já votou nesta pauta", cause);
    }
}
