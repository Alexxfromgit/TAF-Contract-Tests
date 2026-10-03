package io.github.alexxfromgit.taf.contract.core.http.auth;

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/** Applies an {@link AuthProvider} right before the request is sent. */
public final class AuthFilter implements OrderedFilter {

    private final AuthProvider provider;

    public AuthFilter(AuthProvider provider) {
        this.provider = provider;
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
                           FilterContext context) {
        provider.apply(request);
        return context.next(request, response);
    }

    @Override
    public int getOrder() {
        return OrderedFilter.DEFAULT_PRECEDENCE;
    }
}
