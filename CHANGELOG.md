# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - 2026-10-01

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
