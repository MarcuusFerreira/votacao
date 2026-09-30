package br.com.marcusferreira.voting.member;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import br.com.marcusferreira.voting.AbstractIntegrationTest;
import br.com.marcusferreira.voting.common.exception.MemberVerificationUnavailableException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;

/**
 * Proves that the real MemberEligibilityClient (built from the auto-configured RestClient.Builder)
 * honors the read timeout from spring.http.clients.*: an upstream that accepts the connection but never
 * responds results in an error instead of holding the thread indefinitely.
 */
@TestPropertySource(properties = "spring.http.clients.read-timeout=500ms")
class MemberEligibilityClientTimeoutIntegrationTest extends AbstractIntegrationTest {

    // Socket that accepts connections (through the OS backlog) but never sends a response.
    static final ServerSocket HUNG_UPSTREAM = openSocket();

    private static ServerSocket openSocket() {
        try {
            return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("voting.member.base-url",
            () -> "http://127.0.0.1:" + HUNG_UPSTREAM.getLocalPort());
    }

    @AfterAll
    static void closeSocket() throws IOException {
        HUNG_UPSTREAM.close();
    }

    @Autowired
    MemberEligibilityClient client;

    @Test
    void unresponsiveUpstreamFailsWithReadTimeout() {
        assertTimeoutPreemptively(Duration.ofSeconds(4), () ->
            assertThatThrownBy(() -> client.checkEligibility("12345678900"))
                .isInstanceOf(MemberVerificationUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class));
    }
}
