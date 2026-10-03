package io.github.alexxfromgit.taf.contract.core.http.auth;

import io.restassured.specification.FilterableRequestSpecification;

/** {@code auth.type=none}: sends requests without credentials. */
public final class NoAuth implements AuthProvider {

    @Override
    public void apply(FilterableRequestSpecification request) {
        // nothing to add
    }
}
