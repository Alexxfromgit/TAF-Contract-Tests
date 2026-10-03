package io.github.alexxfromgit.taf.contract.core.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.PathType;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin wrapper over networknt json-schema-validator, so the rest of the framework never depends on
 * its API directly. The draft (04, 06, 07, 2019-09, 2020-12) is detected from {@code $schema};
 * schemas without {@code $schema} are treated as 2020-12.
 */
public final class SchemaValidator {

    private static final Map<String, SchemaRegistry> REGISTRIES = new ConcurrentHashMap<>();
    private static final Map<String, Schema> COMPILED = new ConcurrentHashMap<>();

    private SchemaValidator() {
    }

    /**
     * Validates {@code instance} against {@code schema}.
     *
     * @param cacheKey stable key for the compiled schema (e.g. its path plus mode), or {@code null} to skip caching
     */
    public static List<Violation> validate(String cacheKey, JsonNode schema, JsonNode instance,
                                           boolean formatAssertions) {
        Schema compiled = cacheKey == null
                ? compile(schema, formatAssertions)
                : COMPILED.computeIfAbsent(cacheKey + "|" + formatAssertions, k -> compile(schema, formatAssertions));
        return compiled.validate(instance).stream().map(SchemaValidator::toViolation).toList();
    }

    public static SpecificationVersion detectVersion(JsonNode schema) {
        if (schema.hasNonNull("$schema")) {
            return SpecificationVersion.fromSchemaNode(schema).orElseThrow(() -> new FrameworkException(
                    "Unsupported $schema '" + schema.get("$schema").asText() + "'"));
        }
        return SpecificationVersion.DRAFT_2020_12;
    }

    /** Forgets compiled schemas, e.g. after a schema file was (re)recorded. */
    public static void clearCache() {
        COMPILED.clear();
    }

    private static Schema compile(JsonNode schema, boolean formatAssertions) {
        SpecificationVersion version = detectVersion(schema);
        SchemaRegistry registry = REGISTRIES.computeIfAbsent(version + "|" + formatAssertions,
                k -> SchemaRegistry.withDefaultDialect(version, builder -> builder.schemaRegistryConfig(
                        SchemaRegistryConfig.builder()
                                .pathType(PathType.JSON_PATH)
                                .locale(Locale.ENGLISH)
                                .formatAssertionsEnabled(formatAssertions)
                                .build())));
        try {
            return registry.getSchema(schema);
        } catch (RuntimeException e) {
            throw new FrameworkException("Invalid JSON Schema: " + e.getMessage(), e);
        }
    }

    private static Violation toViolation(Error error) {
        String path = error.getInstanceLocation() == null ? "$" : error.getInstanceLocation().toString();
        return new Violation(path, error.getKeyword(), error.getMessage(), error.getProperty());
    }
}
