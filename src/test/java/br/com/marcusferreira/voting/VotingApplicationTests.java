package br.com.marcusferreira.voting;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.common.VotingProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Import(VotingApplicationTests.ThreadProbeController.class)
class VotingApplicationTests extends AbstractIntegrationTest {

    @RestController
    static class ThreadProbeController {
        @GetMapping("/test/thread-is-virtual")
        boolean threadIsVirtual() {
            return Thread.currentThread().isVirtual();
        }
    }

    @Autowired
    HttpClientSettings httpClientSettings;

    @Autowired
    VotingProperties votingProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void httpClientsHaveConfiguredTimeouts() {
        assertThat(httpClientSettings.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(httpClientSettings.readTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void requestsAreHandledOnVirtualThreads() {
        assertThat(restTemplate.getForObject("/test/thread-is-virtual", Boolean.class)).isTrue();
    }

    @Test
    void cpfVerificationDisabledByDefault() {
        assertThat(votingProperties.member().verificationEnabled()).isFalse();
    }
}
