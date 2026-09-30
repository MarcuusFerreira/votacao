package br.com.marcusferreira.voting.common.exception;

public class MemberNotEligibleException extends RuntimeException {
    public MemberNotEligibleException(String cpf) {
        super("Associado com CPF " + cpf + " não está apto a votar");
    }
}
