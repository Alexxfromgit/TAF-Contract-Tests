package io.github.alexxfromgit.taf.contract.examples.graphql;

import io.github.alexxfromgit.taf.contract.core.contract.schema.ExpectedSchema;
import io.github.alexxfromgit.taf.contract.core.graphql.GraphQlResponse;
import io.github.alexxfromgit.taf.contract.core.testng.Groups;
import io.github.alexxfromgit.taf.contract.core.verify.Verify;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.testng.annotations.Test;

import java.util.Map;

import static io.github.alexxfromgit.taf.contract.examples.support.Epics.COUNTRIES;
import static io.github.alexxfromgit.taf.contract.examples.support.Owners.API_TEAM;

/**
 * GraphQL contract tests: the same {@code @ExpectedSchema} mechanism, pointed at the {@code data} payload
 * with {@code jsonPointer}. GraphQL answers HTTP 200 even for failed operations, so errors are checked explicitly.
 */
@Epic(COUNTRIES)
@Feature("Countries")
@Owner(API_TEAM)
public class CountriesContractTest {

    private final CountriesClient countries = new CountriesClient();

    @Test(groups = Groups.SMOKE, description = "Country by code matches the consumer schema")
    @ExpectedSchema(value = "schemas/countries/country.json", jsonPointer = "/data/country")
    public void countryByCode() {
        GraphQlResponse response = countries.countryByCode("UA").assertNoErrors();

        Verify.equal(response.data().at("/country/capital").asText(), "Kyiv", "Capital of Ukraine");
    }

    @Test(description = "Continent lists its countries")
    @ExpectedSchema(value = "schemas/countries/continent-countries.json", jsonPointer = "/data/continent")
    public void countriesByContinent() {
        countries.countriesByContinent("EU").assertNoErrors();
    }

    @Test(description = "Querying a field that does not exist is rejected with a GraphQL error")
    public void unknownFieldIsRejected() {
        GraphQlResponse response = countries.execute(
                "graphql/countries/country-with-unknown-field.graphql", Map.of("code", "UA"));

        Verify.that(!response.errors().isEmpty(), "Response contains GraphQL errors");
        Verify.that(response.errors().get(0).contains("population"), "Error names the unknown field");
    }
}
