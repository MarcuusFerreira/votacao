package br.com.marcusferreira.voting.common;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Keyed hash (HMAC-SHA256) of a CPF. Votes store only this hash: it is enough to enforce one vote
 * per CPF in each session, and without the key the stored value cannot be traced back to the CPF
 * by hashing the (small) space of possible CPFs.
 */
@Component
public class CpfHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public CpfHasher(@Value("${voting.cpf-hash-key}") String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("voting.cpf-hash-key must be set");
        }
        this.key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(String cpf) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(cpf.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
