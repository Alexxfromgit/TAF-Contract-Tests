# Configuration

## Layers

Configuration is a set of `.properties` layers. Later layers win.

| # | Layer | Typical use |
|---|---|---|
| 1 | `taf/defaults.properties` (inside taf-core) | Framework defaults (listed below) |
| 2 | `src/test/resources/taf.properties` | Project settings shared by all environments |
| 3 | `src/test/resources/env/<env>.properties` | Per-environment URLs, selected with `-Denv=<env>` or `TAF_ENV` |
| 4 | System properties | `-Dkey=value` on the command line |
| 5 | Environment variables | `SERVICES_PETSTORE_BASE_URI=...` overrides `services.petstore.base-uri` |
| 6 | Runtime overrides | Values known only during the run, e.g. `stub.base-url` |

Environment variable names are the key in upper case with every non-alphanumeric character replaced by `_`. They
only override keys declared in a file (layers 1-3).

The environment is selected by the system property `taf.env` (or the `TAF_ENV` environment variable). The examples
POM maps the Maven property `-Denv=<name>` to it, so `./mvnw verify -Denv=staging` works from the repository root
without affecting taf-core's own unit tests.

Values can reference other keys: `services.petstore.base-uri=${stub.base-url}/api/v3`. Placeholders are resolved
on every lookup, so runtime values such as the random stub port work.

## Secrets

Secrets are **never** read from files. `TafConfig.secret("services.api.auth.token")` reads the environment variable
`SERVICES_API_AUTH_TOKEN`, falling back to the system property for local runs.

Two safety nets:
- loading fails if a `.properties` layer has a literal value for a secret-looking key (`password`, `secret`,
  `token`, `api-key`, ...). Empty values, `${placeholders}` and `true`/`false` switches are fine;
- the metadata linter scans every `.properties` file of the project for the same.

In GitHub Actions:

```yaml
- run: ./mvnw -B verify -Denv=staging
  env:
    SERVICES_ORDERS_AUTH_CLIENT_SECRET: ${{ secrets.ORDERS_CLIENT_SECRET }}
```

## Services

```properties
services.<id>.base-uri=https://api.example.com/v1     # required; include the OpenAPI server base path
services.<id>.openapi=classpath:openapi/orders.json  # optional: OpenAPI validation + coverage
services.<id>.openapi.validate=true                  # set false to keep coverage but skip validation
services.<id>.content-type=application/json
services.<id>.accept=application/json
services.<id>.graphql-path=/graphql                  # GraphQlClient only

services.<id>.auth.type=none | bearer | api-key | basic | oauth2 | custom
```

| `auth.type` | Keys | Secret (env var) |
|---|---|---|
| `bearer` | - | `SERVICES_<ID>_AUTH_TOKEN` |
| `api-key` | `auth.header` (default `X-API-Key`), `auth.prefix`, `auth.in=header\|query`, `auth.param` (default `api_key`) | `SERVICES_<ID>_AUTH_TOKEN` |
| `basic` | `auth.username` | `SERVICES_<ID>_AUTH_PASSWORD` |
| `oauth2` | `auth.token-url`, `auth.client-id`, `auth.scope`, `auth.client-auth=body\|basic`, `auth.refresh-skew` (default `30s`) | `SERVICES_<ID>_AUTH_CLIENT_SECRET` |
| `custom` | `auth.class` = your `AuthProvider` (constructor `(String serviceId)` or no-args) | your choice |

Providers are cached per service, so one OAuth2 token is shared by all clients of that service.
`AuthProviders.register(id, provider)` replaces a provider programmatically.

## Framework defaults

| Key | Default | Meaning |
|---|---|---|
| `http.connect-timeout` / `http.read-timeout` | `10s` / `30s` | Durations accept `500ms`, `10s`, `2m`, `PT1M` |
| `http.masked-headers` | | Extra headers hidden in Allure (`Authorization`, `Cookie`, `Set-Cookie`, `X-API-Key`, `auth.header` are always hidden) |
| `verify.mode` | `hard` | `soft` collects all `Verify` failures of a test |
| `retry.max` | `1` | Retries per test, only for infrastructure failures |
| `retry.on` | EnvironmentException, ConnectException, SocketTimeoutException, ... | Retried exception types (whole cause chain) |
| `contract.record` | `off` | `missing` / `overwrite`: generate schemas from responses |
| `contract.schema.format-assertions` | `true` | Assert `format` (date-time, email, ...) |
| `contract.drift.enabled` | `true` | Collect undeclared fields |
| `contract.drift.fail` | `false` | Fail the drift report test when drift exists |
| `contract.coverage.fail-under` | `0` | Minimum API coverage per service, in % |
| `contract.openapi.level.<message key>` | `...additionalProperties=WARN` | `ERROR` / `WARN` / `INFO` / `IGNORE` |
| `stub.enabled` | `false` | Start the embedded WireMock |
| `stub.port` / `stub.root` | `0` (random) / `wiremock` | Port and classpath folder with `mappings/` and `__files/` |
| `lint.require-owner` / `lint.require-feature` / `lint.secrets` | `true` | Linter rules |
| `lint.quarantine.max-days` | `90` | Longest allowed `@Quarantined` |
| `taf.secrets-guard.enabled` | `true` | Fail when config files contain secret values |

## Build properties

The examples POM passes these to the tests:

| Property | Default | |
|---|---|---|
| `env` | `stub` | Environment file to load |
| `suite` | `suites/all.xml` | TestNG suite (relative to `src/test/resources`) |
| `allure.results.directory` | `target/allure-results` | |
| `taf.test-resources-dir` | `src/test/resources` | Where record mode writes schemas |
| `taf.reports-dir` | `target/taf-reports` | Coverage and drift files |
