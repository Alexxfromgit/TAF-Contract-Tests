package io.github.alexxfromgit.taf.contract.core.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.github.alexxfromgit.taf.contract.core.report.Json;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Loads schema files from the classpath (e.g. {@code schemas/petstore/pet.json}) and caches them. */
public final class SchemaRepository {

    private static final Map<String, JsonNode> CACHE = new ConcurrentHashMap<>();

    private SchemaRepository() {
    }

    public static Optional<JsonNode> find(String path) {
        JsonNode cached = CACHE.get(path);
        if (cached != null) {
            return Optional.of(cached);
        }
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(stripSlash(path))) {
            if (in == null) {
                return Optional.empty();
            }
            JsonNode schema = Json.MAPPER.readTree(in);
            CACHE.put(path, schema);
            return Optional.of(schema);
        } catch (IOException e) {
            throw new FrameworkException("Schema " + path + " is not valid JSON: " + e.getMessage(), e);
        }
    }

    /** Registers an in-memory schema, e.g. one that was just recorded. */
    public static void put(String path, JsonNode schema) {
        CACHE.put(path, schema);
        SchemaValidator.clearCache();
    }

    private static String stripSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
