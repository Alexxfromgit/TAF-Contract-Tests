package io.github.alexxfromgit.taf.contract.core.http.auth;

import io.restassured.specification.FilterableRequestSpecification;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Supplier;

/**
 * {@code auth.type=basic}: {@code auth.username} from configuration, password via
 * {@code TafConfig.secret("services.<id>.auth.password")}.
 */
public final class BasicAuth implements AuthProvider {

    private final String username;
    private final Supplier<String> password;

    public BasicAuth(String username, Supplier<String> password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void apply(FilterableRequestSpecification request) {
        String credentials = username + ":" + password.get();
        request.header("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }
}
