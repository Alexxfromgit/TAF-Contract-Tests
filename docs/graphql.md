# GraphQL

GraphQL contracts use the same tools as REST: a client, `.graphql` documents and `@ExpectedSchema` on the
`data` part of the response.

## Client

```java
public class CountriesClient extends GraphQlClient {
    public CountriesClient() { super("countries"); }

    public GraphQlResponse countryByCode(String code) {
        return execute("graphql/countries/country-by-code.graphql", Map.of("code", code));
    }
}
```

```properties
services.countries.base-uri=https://countries.trevorblades.com
services.countries.graphql-path=/graphql
# auth.* works as for REST services
```

Documents live in `src/test/resources/graphql/`. The operation name is read from the document
(`query CountryByCode(...)`) and sent as `operationName`. Stubs can match on it:

```json
"bodyPatterns": [{"matchesJsonPath": "$[?(@.operationName == 'CountryByCode')]"}]
```

## Assertions

GraphQL usually returns HTTP 200 even when an operation fails, so check errors explicitly:

```java
countries.countryByCode("UA").assertNoErrors();             // PotentialDefectException listing every error
List<String> errors = response.errors();                     // for negative tests
JsonNode capital = response.data().at("/country/capital");
```

HTTP 5xx responses throw `EnvironmentException`.

## Contracts

```java
@Test
@ExpectedSchema(value = "schemas/countries/country.json", jsonPointer = "/data/country")
public void countryByCode() {
    countries.countryByCode("UA").assertNoErrors();
}
```

Because a GraphQL query states exactly which fields it wants, a schema for the queried shape is a precise contract.
Record mode works here too: `-Dcontract.record=missing` writes the schema of the `jsonPointer` part.

## Roadmap

- Introspection-based checks that every queried field still exists in the server schema (graphql-java).
- Deprecation report for fields marked `@deprecated` in the server schema.
