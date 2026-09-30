package br.com.marcusferreira.voting.common.exception;

public class MemberVerificationUnavailableException extends RuntimeException {
    public MemberVerificationUnavailableException(Throwable cause) {
        super("Serviço de verificação de CPF indisponível. Tente novamente em instantes.", cause);
    }
}
