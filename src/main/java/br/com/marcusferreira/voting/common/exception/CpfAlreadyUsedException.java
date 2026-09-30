package br.com.marcusferreira.voting.common.exception;

import br.com.marcusferreira.voting.common.CpfMask;

public class CpfAlreadyUsedException extends RuntimeException {
    public CpfAlreadyUsedException(String cpf) {
        super("O CPF " + CpfMask.mask(cpf) + " já foi utilizado para votar nesta pauta");
    }

    public CpfAlreadyUsedException(String cpf, Throwable cause) {
        super("O CPF " + CpfMask.mask(cpf) + " já foi utilizado para votar nesta pauta", cause);
    }
}
