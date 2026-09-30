package br.com.marcusferreira.voting.member;

import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.InvalidCpfException;
import br.com.marcusferreira.voting.common.exception.MemberNotEligibleException;
import br.com.marcusferreira.voting.common.exception.MemberVerificationUnavailableException;
import br.com.marcusferreira.voting.member.dto.MemberStatusResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MemberEligibilityClient {

    private final RestClient restClient;

    public MemberEligibilityClient(RestClient.Builder builder, VotingProperties properties) {
        this.restClient = builder.baseUrl(properties.member().baseUrl()).build();
    }

    public void checkEligibility(String cpf) {
        try {
            MemberStatusResponse response = restClient.get()
                .uri("/users/{cpf}", cpf)
                .retrieve()
                .body(MemberStatusResponse.class);
            if (response == null || response.status() == MemberStatus.UNABLE_TO_VOTE) {
                throw new MemberNotEligibleException(cpf);
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new InvalidCpfException(cpf);
        } catch (RestClientException e) {
            // Fail closed: when eligibility cannot be confirmed (timeout, 5xx, unexpected status or
            // payload) the vote is refused with 503 rather than accepted unverified.
            throw new MemberVerificationUnavailableException(e);
        }
    }
}
