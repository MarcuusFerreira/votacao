package br.com.marcusferreira.voting.member;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.common.exception.InvalidCpfException;
import br.com.marcusferreira.voting.common.exception.MemberNotEligibleException;
import br.com.marcusferreira.voting.common.exception.MemberVerificationUnavailableException;
import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MemberEligibilityClientTest {

    private VotingProperties properties(String baseUrl) {
        return new VotingProperties("http://localhost:8080", ZoneId.of("America/Sao_Paulo"),
            new VotingProperties.Session(Duration.ofSeconds(60)),
            new VotingProperties.Member(baseUrl, true));
    }

    @Test
    void eligibleCpfDoesNotThrow() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MemberEligibilityClient client = new MemberEligibilityClient(builder, properties("http://user-info.test"));

        server.expect(requestTo("http://user-info.test/users/12345678900"))
            .andRespond(withSuccess("{\"status\":\"ABLE_TO_VOTE\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.checkEligibility("12345678900")).doesNotThrowAnyException();
    }

    @Test
    void ineligibleCpfThrowsMemberNotEligibleException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MemberEligibilityClient client = new MemberEligibilityClient(builder, properties("http://user-info.test"));

        server.expect(requestTo("http://user-info.test/users/98765432100"))
            .andRespond(withSuccess("{\"status\":\"UNABLE_TO_VOTE\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.checkEligibility("98765432100"))
            .isInstanceOf(MemberNotEligibleException.class);
    }

    @Test
    void unknownCpfThrowsInvalidCpfException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MemberEligibilityClient client = new MemberEligibilityClient(builder, properties("http://user-info.test"));

        server.expect(requestTo("http://user-info.test/users/00000000000"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.checkEligibility("00000000000"))
            .isInstanceOf(InvalidCpfException.class);
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE", "TOO_MANY_REQUESTS", "BAD_REQUEST"})
    void unexpectedStatusFromServiceMeansVerificationUnavailable(HttpStatus status) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MemberEligibilityClient client = new MemberEligibilityClient(builder, properties("http://user-info.test"));

        server.expect(requestTo("http://user-info.test/users/12345678900")).andRespond(withStatus(status));

        assertThatThrownBy(() -> client.checkEligibility("12345678900"))
            .isInstanceOf(MemberVerificationUnavailableException.class);
    }
}
