package io.github.alexxfromgit.taf.contract.core.http.auth;

import io.restassured.specification.FilterableRequestSpecification;

import java.util.function.Supplier;

/**
 * Static token in a header or query parameter.
 * <ul>
 *     <li>{@code auth.type=bearer}: {@code Authorization: Bearer <token>}</li>
 *     <li>{@code auth.type=api-key}: {@code <auth.header, default X-API-Key>: <token>}, or a query
 *     parameter when {@code auth.in=query} ({@code auth.param}, default {@code api_key})</li>
 * </ul>
 * The token is read with {@code TafConfig.secret("services.<id>.auth.token")}.
 */
public final class HeaderTokenAuth implements AuthProvider {

    private final String name;
    private final String prefix;
    private final boolean inQuery;
    private final Supplier<String> token;

    public HeaderTokenAuth(String name, String prefix, boolean inQuery, Supplier<String> token) {
        this.name = name;
        this.prefix = prefix;
        this.inQuery = inQuery;
        this.token = token;
    }

    public static HeaderTokenAuth bearer(Supplier<String> token) {
        return new HeaderTokenAuth("Authorization", "Bearer ", false, token);
    }

    @Override
    public void apply(FilterableRequestSpecification request) {
        if (inQuery) {
            request.queryParam(name, token.get());
        } else {
            request.header(name, prefix + token.get());
        }
    }
}
