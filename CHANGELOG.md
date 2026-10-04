# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.1] - 2026-10-04

### Fixed
- `-Denv=...` from the repository root no longer leaks into taf-core's unit tests: the framework now reads the
  `taf.env` system property (the examples POM maps `-Denv` to it). `./mvnw verify -Denv=live` works again.
- Retry log messages show the failure instead of a literal `{}`.

### Upgrade notes
- If your own module passes the environment to the tests itself, rename the system property from `env` to
  `taf.env` (in the examples POM: `<taf.env>${env}</taf.env>`). The `TAF_ENV` environment variable and the
  `env=` key in `taf.properties` work as before.

## [1.0.0] - 2026-10-03

### Added
- Layered configuration with environment files, environment variable overrides, placeholders and a secrets guard.
- `ServiceClient` with Allure request/response attachments (credentials masked), call recording and pluggable auth
  (`none`, `bearer`, `api-key`, `basic`, `oauth2` client credentials, `custom`).
- JSON Schema contracts: `@ExpectedSchema`, `ContractAssert`, consumer-tolerant default, strict mode, draft detection.
- OpenAPI validation of every exchange with configurable message levels.
- Drift report of undeclared response fields (from schemas and OpenAPI).
- API coverage report with `@Covers` and a `fail-under` gate.
- Record mode that infers schemas from live responses.
- GraphQL client with document files, error assertions and schema checks on `data`.
- Embedded WireMock for offline, deterministic runs.
- Failure taxonomy mapped to Allure categories, `Verify` (soft/hard), `Log` steps, `@KnownIssue`, `@Quarantined`,
  infrastructure-only retries, severity from groups and a metadata linter.
- Examples for Swagger Petstore, Countries GraphQL and OAuth2; CI with GitHub Pages report publishing.

[Unreleased]: https://github.com/Alexxfromgit/TAF-Contract-Tests/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/Alexxfromgit/TAF-Contract-Tests/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/Alexxfromgit/TAF-Contract-Tests/releases/tag/v1.0.0
