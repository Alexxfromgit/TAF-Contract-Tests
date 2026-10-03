package io.github.alexxfromgit.taf.contract.core.http.auth;

import io.github.alexxfromgit.taf.contract.core.failure.EnvironmentException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.RequestSpecification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * {@code auth.type=oauth2}: OAuth 2.0 client-credentials grant (Keycloak, Auth0, Entra ID, ...).
 * <pre>
 * services.api.auth.token-url=https://idp.example.com/oauth/token
 * services.api.auth.client-id=my-client
 * services.api.auth.scope=read write        # optional
 * services.api.auth.client-auth=body        # body (default) | basic
 * # secret: environment variable SERVICES_API_AUTH_CLIENT_SECRET
 * </pre>
 * The token is cached and refreshed shortly before it expires; the token request itself is
 * deliberately not attached to Allure so the client secret never appears in reports.
 */
public final class OAuth2ClientCredentialsAuth implements AuthProvider {

    private static final Duration DEFAULT_LIFETIME = Duration.ofMinutes(5);

    private final String tokenUrl;
    private final String clientId;
    private final Supplier<String> clientSecret;
    private final String scope;
    private final boolean basicClientAuth;
    private final Duration refreshSkew;
    private final Clock clock;
    private final AtomicReference<CachedToken> token = new AtomicReference<>();

    public OAuth2ClientCredentialsAuth(String tokenUrl, String clientId, Supplier<String> clientSecret, String scope,
                                       boolean basicClientAuth, Duration refreshSkew, Clock clock) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.basicClientAuth = basicClientAuth;
        this.refreshSkew = refreshSkew;
        this.clock = clock;
    }

    @Override
    public void apply(FilterableRequestSpecification request) {
        request.header("Authorization", "Bearer " + accessToken());
    }

    /** Current access token, fetching a new one when missing or about to expire. */
    public String accessToken() {
        CachedToken current = token.get();
        if (current != null && current.isValidAt(clock.instant().plus(refreshSkew))) {
            return current.value();
        }
        synchronized (this) {
            current = token.get();
            if (current == null || !current.isValidAt(clock.instant().plus(refreshSkew))) {
                current = fetch();
                token.set(current);
            }
            return current.value();
        }
    }

    private CachedToken fetch() {
        RequestSpecification spec = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials");
        if (basicClientAuth) {
            spec.auth().preemptive().basic(clientId, clientSecret.get());
        } else {
            spec.formParam("client_id", clientId).formParam("client_secret", clientSecret.get());
        }
        if (scope != null && !scope.isBlank()) {
            spec.formParam("scope", scope);
        }
        Response response;
        try {
            response = spec.post(tokenUrl);
        } catch (RuntimeException e) {
            throw new EnvironmentException("Token endpoint " + tokenUrl + " is not reachable", e);
        }
        if (response.statusCode() != 200) {
            throw new EnvironmentException("Token endpoint " + tokenUrl + " returned HTTP "
                    + response.statusCode() + " for client '" + clientId + "'");
        }
        String accessToken = response.path("access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new EnvironmentException("Token endpoint " + tokenUrl + " returned no access_token");
        }
        Number expiresIn = response.path("expires_in");
        Duration lifetime = expiresIn == null ? DEFAULT_LIFETIME : Duration.ofSeconds(expiresIn.longValue());
        return new CachedToken(accessToken, clock.instant().plus(lifetime));
    }

    private record CachedToken(String value, Instant expiresAt) {
        boolean isValidAt(Instant instant) {
            return instant.isBefore(expiresAt);
        }
    }
}
