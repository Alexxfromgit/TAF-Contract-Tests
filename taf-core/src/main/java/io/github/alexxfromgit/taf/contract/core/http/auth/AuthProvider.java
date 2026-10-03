package io.github.alexxfromgit.taf.contract.core.http.auth;

import io.restassured.specification.FilterableRequestSpecification;

/**
 * Adds credentials to an outgoing request. Built-in implementations are selected with
 * {@code services.<id>.auth.type}; for anything else (cookies, signed requests, SSO flows)
 * implement this interface and set {@code auth.type=custom} + {@code auth.class=<your class>}.
 * Custom classes need a public constructor taking the service id ({@code String}) or no arguments.
 */
@FunctionalInterface
public interface AuthProvider {

    void apply(FilterableRequestSpecification request);
}
