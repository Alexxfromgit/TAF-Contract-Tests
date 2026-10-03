# Contract testing guide

A *contract* is what a consumer (your app, another service) relies on in a provider's API. contract-taf checks
contracts at three levels:

| Level | Question | Source of truth | Fails the test? |
|---|---|---|---|
| JSON Schema (`@ExpectedSchema`) | Does the response contain what **we** use? | Schemas written (or recorded) by the consumer | Yes |
| OpenAPI validation | Does the exchange match what the **provider** documents? | The provider's OpenAPI document | ERROR messages: yes |
| Drift | Did the provider **add** something we do not know about? | Both of the above | No (report only) |

## JSON Schema contracts

### Annotation

```java
@Test
@ExpectedSchema("schemas/petstore/pet.json")
public void getPet() {
    pets.getPet(10).then().statusCode(200);
}
```

After the test body passes, the listener picks a recorded call and validates its body:

- by default, the **last JSON response** of the test;
- `endpoint = "GET /pet/{petId}"` selects the last call matching that method and path template, which is useful when
  the test makes several calls;
- `jsonPointer = "/data/country"` validates only a part of the body, as for GraphQL or response envelopes;
- the annotation is repeatable, so one test can check several calls.

Only calls made through a `ServiceClient` are recorded.

### Fluent API

```java
Response response = pets.addPet(pet);
ContractAssert.assertThat(response).strict().matchesSchema("schemas/petstore/pet.json");
```

Use it when the test needs the check at a specific point, or for payloads that do not come from HTTP
(`ContractAssert.assertThat(JsonNode)`).

### Tolerant vs. strict

Schemas are **consumer-tolerant** by default: describe the fields the consumer uses (`required` + types) and
nothing else. Extra fields pass. That is what a consumer-driven contract means, and it lets providers add fields
without breaking consumers.

`strict = true` (or `.strict()`) rejects any property the schema does not declare. The schema file is not edited.
The framework adds `additionalProperties: false` to every object schema at validation time, or
`unevaluatedProperties: false` where `allOf`/`anyOf`/`oneOf` are used in 2019-09+ schemas. Use strict mode where
the full shape matters, for example an echo of what you sent, or responses you cache or persist.

### Writing schemas

- Prefer draft 2020-12 (`"$schema": "https://json-schema.org/draft/2020-12/schema"`). The draft is detected per
  file, so older schemas keep working.
- Only `required` what the consumer really needs. Use `["string", "null"]` for nullable fields.
- Use `$defs` + `$ref` (`"#/$defs/pet"`) for reuse inside a file.
- Formats (`date-time`, `email`, `uuid`, ...) are asserted (`contract.schema.format-assertions=true`).
- Avoid pinning data values (`enum` of IDs, exact sizes) unless the value set really is part of the contract.

### Error messages

```
Response of GET /pet/{petId} does not match schemas/petstore/pet.json - 2 violation(s):
  $.name -> required: required property 'name' not found
  $.tags[0].id -> type: integer found, string expected
```

The schema, the payload and the full violation list are attached to the Allure step.

## Record mode

Record mode drafts schemas from real responses:

```bash
./mvnw verify -Dcontract.record=missing     # only schemas that do not exist yet
./mvnw verify -Dcontract.record=overwrite   # all schemas (after an intended API change)
```

When a schema is missing, it is inferred from the actual response and written to `src/test/resources/<path>`, and
the test is **skipped** with "Schema recorded - review it and commit". The inference rules are:

- properties: the union of keys seen (array items are merged);
- `required`: keys present and non-null in every sample;
- a value seen as `null` becomes `["<type>", "null"]`; `integer` + `number` become `number`;
- no `additionalProperties`, so the schema stays tolerant.

Then review it. Remove fields the consumer does not use from `required`, and add `enum`, `format` and `pattern`
where it helps. `schemas/petstore/recorded/inventory.json` in the examples was produced this way.

## OpenAPI validation

```properties
services.petstore.openapi=classpath:openapi/petstore-openapi.json   # or https://.../openapi.json, or a file path
```

With this set, every request and response of the service is validated by the
[Atlassian OpenAPI validator](https://bitbucket.org/atlassian/swagger-request-validator). ERROR messages throw a
`ContractViolationException` at the call. WARN/INFO messages are attached to the step.

Tune levels per message key:

```properties
contract.openapi.level.validation.request.security.missing=IGNORE
contract.openapi.level.validation.response.body.missing=WARN
```

The framework default lowers `validation.response.body.schema.additionalProperties` to WARN: undeclared fields are
drift, not breakage. Disable validation for one service with `services.<id>.openapi.validate=false`.

A snapshot of the document in the repository gives reproducible runs. The provider's live URL checks against what
they publish today. Limitation: OpenAPI 3.1 support of the validator is partial, so prefer 3.0.x documents.

## Drift detection

After a tolerant schema check passes, the framework re-runs it in strict mode **without failing** and records every
undeclared field. The OpenAPI filter does the same for its additional-property warnings. The result is collected
per endpoint and written by the *API drift* report:

| Endpoint | Undeclared field | Not declared in | Seen in tests |
|---|---|---|---|
| GET /pet/{petId} | `$.nickname` | openapi:petstore, schemas/petstore/pet.json | PetDriftDemoTest.petWithUndeclaredFields |
| GET /user/{username} | `$.password` | schemas/petstore/user.json | UserContractTest.getUser |

A renamed field shows up as one failure (the old name is missing) plus one drift entry (the new name). Set `contract.drift.fail=true` to turn drift into a failure, or `contract.drift.enabled=false` to switch
it off.

## API coverage

For every service with an OpenAPI document, the *API coverage* report lists each documented endpoint, whether it
was exercised, which status codes were seen, and calls to endpoints **not** in the document (often an outdated
spec).

- Calls through a `ServiceClient` are counted automatically.
- Calls made another way can be declared with `@Covers(method = "POST", path = "/pet/{petId}/uploadImage")`.
- `contract.coverage.fail-under=80` makes the *API coverage* test fail below 80% for any service.

The reports are produced by `ContractReports`. Keep it as the last `<test>` of your suites.
