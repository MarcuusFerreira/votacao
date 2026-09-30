package br.com.marcusferreira.voting.common.exception;

import br.com.marcusferreira.voting.common.CpfMask;

public class InvalidCpfException extends RuntimeException {
    public InvalidCpfException(String cpf) {
        super("CPF inválido: " + CpfMask.mask(cpf));
    }
}
