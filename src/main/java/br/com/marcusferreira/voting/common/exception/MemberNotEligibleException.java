package br.com.marcusferreira.voting.common.exception;

import br.com.marcusferreira.voting.common.CpfMask;

public class MemberNotEligibleException extends RuntimeException {
    public MemberNotEligibleException(String cpf) {
        super("Associado com CPF " + CpfMask.mask(cpf) + " não está apto a votar");
    }
}
