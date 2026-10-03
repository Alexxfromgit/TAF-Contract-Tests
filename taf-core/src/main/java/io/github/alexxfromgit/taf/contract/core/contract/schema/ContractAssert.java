package io.github.alexxfromgit.taf.contract.core.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.alexxfromgit.taf.contract.core.allure.AllureResults;
import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.contract.coverage.EndpointCatalogs;
import io.github.alexxfromgit.taf.contract.core.contract.drift.DriftCollector;
import io.github.alexxfromgit.taf.contract.core.contract.record.RecordMode;
import io.github.alexxfromgit.taf.contract.core.contract.record.SchemaRecorder;
import io.github.alexxfromgit.taf.contract.core.failure.ContractViolationException;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.github.alexxfromgit.taf.contract.core.http.CallRecord;
import io.github.alexxfromgit.taf.contract.core.report.Json;
import io.github.alexxfromgit.taf.contract.core.testng.TestContext;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.testng.SkipException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fluent JSON Schema assertion.
 * <pre>{@code
 * ContractAssert.assertThat(response).matchesSchema("schemas/petstore/pet.json");
 * ContractAssert.assertThat(response).at("/data/country").strict().matchesSchema("schemas/countries/country.json");
 * }</pre>
 * Failures throw {@link ContractViolationException} with one readable line per violation and attach the schema,
 * the payload and the violations to Allure. In tolerant mode, undeclared fields are sent to the drift report.
 */
public final class ContractAssert {

    private static final int MAX_LINES = 25;
    private static final Set<String> DRIFT_KEYWORDS = Set.of("additionalProperties", "unevaluatedProperties");

    private final JsonNode body;
    private String endpoint;
    private String pointer = "";
    private boolean strict;

    private ContractAssert(JsonNode body, String endpoint) {
        this.body = body;
        this.endpoint = endpoint;
    }

    /** Validates the body of a REST Assured response. */
    public static ContractAssert assertThat(Response response) {
        String label = TestContext.current().lastCall().map(EndpointCatalogs::label).orElse("response");
        return new ContractAssert(parse(response.asString(), label), label);
    }

    public static ContractAssert assertThat(CallRecord call) {
        String label = EndpointCatalogs.label(call);
        return new ContractAssert(parse(call.body(), label), label);
    }

    public static ContractAssert assertThat(JsonNode json) {
        return new ContractAssert(json, "payload");
    }

    /** Validate only the node at this JSON Pointer, e.g. {@code "/data"}. */
    public ContractAssert at(String jsonPointer) {
        this.pointer = jsonPointer == null ? "" : jsonPointer;
        return this;
    }

    public ContractAssert strict() {
        return strict(true);
    }

    public ContractAssert strict(boolean strict) {
        this.strict = strict;
        return this;
    }

    /** Label used in messages and in the drift report, e.g. {@code "GET /pet/{petId}"}. */
    public ContractAssert endpoint(String label) {
        this.endpoint = label;
        return this;
    }

    public ContractAssert matchesSchema(String schemaPath) {
        JsonNode target = pointer.isEmpty() ? body : body.at(pointer);
        if (target.isMissingNode()) {
            throw new ContractViolationException("Response of " + endpoint + " has no node at JSON Pointer '"
                    + pointer + "'. Body: " + abbreviate(body.toString()));
        }

        RecordMode mode = RecordMode.current();
        Optional<JsonNode> schema = mode == RecordMode.OVERWRITE ? Optional.empty() : SchemaRepository.find(schemaPath);
        if (schema.isEmpty()) {
            if (mode == RecordMode.OFF) {
                throw new FrameworkException("Schema '" + schemaPath + "' not found on the classpath. Create it, "
                        + "or run once with -Dcontract.record=missing to generate it from the live response.");
            }
            Path written = SchemaRecorder.record(schemaPath, target);
            throw new SkipException("Schema recorded at " + written + " from " + endpoint
                    + " - review it, commit it and re-run without contract.record");
        }

        String stepName = "Response of " + endpoint + " matches " + schemaPath + (strict ? " (strict)" : "");
        runAsStep(stepName, () -> validate(schemaPath, schema.get(), target));
        return this;
    }

    private void validate(String schemaPath, JsonNode schema, JsonNode target) {
        boolean formats = TafConfig.get().bool("contract.schema.format-assertions", true);
        JsonNode effective = strict ? StrictSchemaTransformer.strict(schema) : schema;
        List<Violation> violations = SchemaValidator.validate(
                schemaPath + (strict ? "#strict" : "#tolerant"), effective, target, formats);

        if (!violations.isEmpty()) {
            attachEvidence(effective, target, violations);
            throw new ContractViolationException(describe(schemaPath, violations));
        }
        if (!strict && TafConfig.get().bool("contract.drift.enabled", true)) {
            detectDrift(schemaPath, schema, target, formats);
        }
    }

    private void detectDrift(String schemaPath, JsonNode schema, JsonNode target, boolean formats) {
        List<Violation> extras = SchemaValidator.validate(schemaPath + "#strict",
                        StrictSchemaTransformer.strict(schema), target, formats).stream()
                .filter(v -> DRIFT_KEYWORDS.contains(v.keyword()))
                .toList();
        if (extras.isEmpty()) {
            return;
        }
        String test = TestContext.current().method()
                .map(m -> m.getDeclaringClass().getSimpleName() + "." + m.getName()).orElse(null);
        Set<String> paths = extras.stream()
                .map(v -> DriftCollector.normalize(prefix() + v.fullPath().substring(1)))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        paths.forEach(path -> DriftCollector.record(endpoint, schemaPath, path, test));
        AllureResults.attachText("Drift: undeclared fields", String.join("\n", paths));
    }

    private String prefix() {
        return pointer.isEmpty() ? "$" : "$" + pointer.replace('/', '.');
    }

    private String describe(String schemaPath, List<Violation> violations) {
        StringBuilder message = new StringBuilder()
                .append("Response of ").append(endpoint).append(" does not match ").append(schemaPath)
                .append(strict ? " (strict)" : "").append(" - ").append(violations.size()).append(" violation(s):");
        violations.stream().limit(MAX_LINES).forEach(v -> message.append("\n  ").append(v));
        if (violations.size() > MAX_LINES) {
            message.append("\n  ... and ").append(violations.size() - MAX_LINES).append(" more (see attachment)");
        }
        return message.toString();
    }

    private static void attachEvidence(JsonNode schema, JsonNode target, List<Violation> violations) {
        AllureResults.attachText("Schema violations",
                violations.stream().map(Violation::toString).collect(Collectors.joining("\n")));
        AllureResults.attachJson("Expected schema", Json.pretty(schema));
        AllureResults.attachJson("Actual payload", Json.pretty(target));
    }

    private static void runAsStep(String name, Runnable check) {
        if (Allure.getLifecycle().getCurrentTestCaseOrStep().isPresent()) {
            Allure.step(name, check::run);
        } else {
            check.run();
        }
    }

    private static JsonNode parse(String text, String label) {
        return Json.tryParse(text).orElseThrow(() -> new ContractViolationException(
                "Response of " + label + " is not JSON: " + abbreviate(text)));
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "<empty>";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
