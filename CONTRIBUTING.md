# Contributing

Thanks for helping! Bug reports, docs fixes and features are all welcome.

## Build

```bash
./mvnw verify            # unit tests + example contract tests against stubs (offline)
./mvnw verify -Denv=live # against the real public APIs (may fail because of the APIs themselves)
```

JDK 21+ is required. CI uses Temurin 21.

## Guidelines

- **Framework code goes into `taf-core`** and must not know about the example APIs. Every behaviour change
  needs a unit test in `taf-core/src/test`.
- **Keep examples small and realistic.**
- Keep the framework free of shared mutable state, because tests run in parallel. Per-test state goes into
  `TestContext`.
- Prefer configuration keys with sensible defaults over new mandatory setup. Document new keys in
  `docs/configuration.md` and `taf/defaults.properties`.
- Never commit secrets, internal hostnames or real personal data. The secrets guard and the linter help, but
  review your diff.
- Commit messages: imperative mood, short subject line (`Add strict mode to ContractAssert`).

## Pull requests

1. Open an issue first for larger changes, so we can agree on the approach.
2. Keep PRs focused, and update `CHANGELOG.md` under *Unreleased*.
3. Make sure `./mvnw verify` is green.

By contributing you agree that your contributions are licensed under the MIT License.
