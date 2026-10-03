# Reports, failure taxonomy and suite hygiene

## Failure taxonomy

Throw the exception that matches why the test could not pass, so it is clear who has to act. Allure groups
failures into categories accordingly (`taf/allure/categories.json`; override it with your own
`src/test/resources/allure/categories.json`).

| Exception | Allure status | Category | Throw it when |
|---|---|---|---|
| `ContractViolationException` | failed | Contract violations | Thrown by the framework on schema/OpenAPI mismatches |
| `PotentialDefectException` (or any `AssertionError`) | failed | Product defects | The product behaves differently from the expectation |
| `KnownIssueException` | failed | Known issues | Wrapped automatically for `@KnownIssue` tests |
| `TestDataException` | broken | Test data problems | Required data is missing or cannot be created |
| `EnvironmentException` | broken | Environment / infrastructure | Dependency down, 5xx from a third party, timeouts |
| `FrameworkException` | broken | Framework problems | Misconfiguration or a bug in the test code |

Connection errors (`ConnectException`, `SocketTimeoutException`, `UnknownHostException`) are categorised as
environment problems automatically.

## Verify

```java
Verify.equal(order.status(), "approved", "Order status");
Verify.that(items.size() > 0, "Basket is not empty");
Verify.softly().assertThat(user.email()).endsWith("@example.org");   // always soft, rich AssertJ API
```

Every check becomes an Allure step. With `verify.mode=soft`, `Verify.that/equal` failures are collected and
reported together when the test body ends. Plain AssertJ `assertThat` is always hard.

`Log.info("Created order {}", id)` writes to the log and adds an Allure step.

## Retries: infrastructure only

`InfraRetryAnalyzer` is attached to every test that has no retry analyzer. It retries (`retry.max`, default 1)
**only** when the cause chain contains one of `retry.on` (EnvironmentException, connection and timeout errors).
Assertion failures are never retried, because retrying a real bug until it passes hides the bug.

## Known issues

```java
@Test
@KnownIssue(id = "42")   // link from allure.link.issue.pattern, or url = "https://..."
public void discountIsApplied() { ... }
```

The test still runs. On failure it is reported under *Known issues* with a link, named `[KNOWN 42] ...`. When it
starts passing, a warning step reminds you to remove the annotation.

## Quarantine

```java
@Quarantined(until = "2026-11-15", reason = "Flaky on staging, see #17")
```

The test is disabled until that date and then runs again automatically, so quarantine cannot silently become
permanent. The linter rejects dates more than `lint.quarantine.max-days` (90) away.

## Metadata linter

`MetadataLinter.forPackages("com.example.tests").assertClean()` checks that:
- every test has `@Owner` (who maintains it?);
- every test has `@Epic`, `@Feature` or `@Story` (where does it belong in the report?);
- every `@Quarantined` is valid and short;
- no `.properties` file contains a secret-looking value.

The examples run it as the first `<test>` of every suite (`MetadataLintTest`), so problems fail fast.

## Severity from groups

Tests in the `blocker`, `critical` or `minor` groups (constants in `Groups`) get the matching Allure severity unless
they have an explicit `@Severity`.

## Framework reports

`ContractReports` (last `<test>` of the suite) produces:

| Test | Attachments | Fails when |
|---|---|---|
| API coverage meets `contract.coverage.fail-under` | `API coverage` (HTML), JSON | coverage < threshold |
| API drift: undeclared response fields | `API drift` (HTML), JSON | drift exists and `contract.drift.fail=true` |

The same files are written to `target/taf-reports/` (also uploaded by CI), even if the suite does not include
`ContractReports`.
