package br.com.marcusferreira.voting;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.marcusferreira.voting.common.VotingProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpClientSettings;

class VotingApplicationTests extends AbstractIntegrationTest {

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
    void cpfVerificationDisabledByDefault() {
        assertThat(votingProperties.member().verificationEnabled()).isFalse();
    }
}
