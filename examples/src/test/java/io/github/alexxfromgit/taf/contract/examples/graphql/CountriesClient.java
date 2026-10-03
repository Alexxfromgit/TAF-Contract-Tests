package io.github.alexxfromgit.taf.contract.examples.graphql;

import io.github.alexxfromgit.taf.contract.core.graphql.GraphQlClient;
import io.github.alexxfromgit.taf.contract.core.graphql.GraphQlResponse;

import java.util.Map;

/** Client for the public Countries GraphQL API (https://countries.trevorblades.com). */
public class CountriesClient extends GraphQlClient {

    public CountriesClient() {
        super("countries");
    }

    public GraphQlResponse countryByCode(String code) {
        return execute("graphql/countries/country-by-code.graphql", Map.of("code", code));
    }

    public GraphQlResponse countriesByContinent(String continentCode) {
        return execute("graphql/countries/countries-by-continent.graphql", Map.of("code", continentCode));
    }
}
