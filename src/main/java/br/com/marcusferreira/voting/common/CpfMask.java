package br.com.marcusferreira.voting.common;

/**
 * Masks a CPF for messages and logs, keeping only the last two digits.
 */
public final class CpfMask {

    private CpfMask() {
    }

    public static String mask(String cpf) {
        if (cpf == null || cpf.length() < 3) {
            return "***";
        }
        return "*".repeat(cpf.length() - 2) + cpf.substring(cpf.length() - 2);
    }
}
