package br.com.marcusferreira.voting.common.exception;

public class InvalidCpfException extends RuntimeException {
    public InvalidCpfException(String cpf) {
        super("CPF inválido: " + cpf);
    }
}
