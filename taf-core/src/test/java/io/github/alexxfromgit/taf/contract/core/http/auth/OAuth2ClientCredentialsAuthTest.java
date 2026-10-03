package io.github.alexxfromgit.taf.contract.core.http.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.alexxfromgit.taf.contract.core.failure.EnvironmentException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OAuth2ClientCredentialsAuthTest {

    private WireMockServer idp;

    @BeforeClass
    public void startIdentityProvider() {
        idp = new WireMockServer(options().dynamicPort());
        idp.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopIdentityProvider() {
        idp.stop();
    }

    @BeforeMethod
    public void reset() {
        idp.resetAll();
        idp.stubFor(post(urlEqualTo("/oauth/token"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_secret=s3cret"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"abc\", \"token_type\": \"Bearer\", \"expires_in\": 300}")));
    }

    @Test
    public void tokenIsFetchedOnceAndReusedUntilExpiry() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        OAuth2ClientCredentialsAuth auth = auth("s3cret", clock);

        for (int i = 0; i < 5; i++) {
            assertThat(auth.accessToken()).isEqualTo("abc");
        }
        idp.verify(1, postRequestedFor(urlEqualTo("/oauth/token")));

        clock.advance(Duration.ofSeconds(280));   // within the 30 s refresh skew of the 300 s lifetime
        auth.accessToken();
        idp.verify(2, postRequestedFor(urlEqualTo("/oauth/token")));
    }

    @Test
    public void rejectedCredentialsAreAnEnvironmentProblem() {
        OAuth2ClientCredentialsAuth auth = auth("wrong", new MutableClock(Instant.now()));
        assertThatThrownBy(auth::accessToken)
                .isInstanceOf(EnvironmentException.class)
                .hasMessageContaining("HTTP 404")
                .hasMessageNotContaining("wrong");
    }

    private OAuth2ClientCredentialsAuth auth(String secret, Clock clock) {
        return new OAuth2ClientCredentialsAuth(idp.baseUrl() + "/oauth/token", "demo-client", () -> secret,
                "read", false, Duration.ofSeconds(30), clock);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
