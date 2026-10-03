# Security policy

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Use GitHub's
[private vulnerability reporting](https://github.com/Alexxfromgit/TAF-Contract-Tests/security/advisories/new) instead.
You will get a response within a few days.

## Secrets in tests

The framework is designed so that credentials never live in the repository:

- secrets are read only from environment variables (`TafConfig.secret`);
- configuration loading fails if a `.properties` file contains a secret-like value;
- the metadata linter scans the project's `.properties` files;
- `Authorization`, cookies and API-key headers are masked in Allure attachments.

If you find a way these safeguards can be bypassed, please report it as above.
