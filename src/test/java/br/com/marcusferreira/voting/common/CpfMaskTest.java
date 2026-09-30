package br.com.marcusferreira.voting.common;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.common.exception.InvalidCpfException;
import br.com.marcusferreira.voting.common.exception.MemberNotEligibleException;
import org.junit.jupiter.api.Test;

class CpfMaskTest {

    @Test
    void keepsOnlyTheLastTwoDigits() {
        assertThat(CpfMask.mask("12345678900")).isEqualTo("*********00");
    }

    @Test
    void handlesMissingOrShortValues() {
        assertThat(CpfMask.mask(null)).isEqualTo("***");
        assertThat(CpfMask.mask("1")).isEqualTo("***");
    }

    @Test
    void exceptionMessagesDoNotExposeTheCpf() {
        assertThat(new InvalidCpfException("12345678900").getMessage()).doesNotContain("12345678900").contains("*********00");
        assertThat(new MemberNotEligibleException("12345678900").getMessage()).doesNotContain("12345678900").contains("*********00");
    }
}
