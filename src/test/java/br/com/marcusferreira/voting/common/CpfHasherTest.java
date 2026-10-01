package br.com.marcusferreira.voting.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CpfHasherTest {

    @Test
    void hashIsDeterministicHexAndDoesNotContainTheCpf() {
        CpfHasher hasher = new CpfHasher("chave-de-teste");

        String hash = hasher.hash("12345678900");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+").doesNotContain("12345678900");
        assertThat(hasher.hash("12345678900")).isEqualTo(hash);
        assertThat(hasher.hash("12345678901")).isNotEqualTo(hash);
    }

    @Test
    void differentKeysProduceDifferentHashes() {
        assertThat(new CpfHasher("chave-a").hash("12345678900"))
            .isNotEqualTo(new CpfHasher("chave-b").hash("12345678900"));
    }

    @Test
    void requiresAKey() {
        assertThatThrownBy(() -> new CpfHasher(" ")).isInstanceOf(IllegalStateException.class);
    }
}
