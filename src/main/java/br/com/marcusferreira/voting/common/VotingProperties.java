package br.com.marcusferreira.voting.common;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voting")
public record VotingProperties(String publicBaseUrl, Session session, Member member) {

    public record Session(Duration defaultDuration) {}

    public record Member(String baseUrl, boolean verificationEnabled) {}
}
