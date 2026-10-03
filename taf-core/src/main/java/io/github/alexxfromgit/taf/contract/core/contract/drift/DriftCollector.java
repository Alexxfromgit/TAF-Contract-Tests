package io.github.alexxfromgit.taf.contract.core.contract.drift;

import io.github.alexxfromgit.taf.contract.core.report.Html;
import io.github.alexxfromgit.taf.contract.core.report.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Suite-wide collection of "drift": fields that responses contain but the expected contract (a JSON Schema or
 * the OpenAPI document) does not declare. Drift never fails a test - consumer-tolerant contracts allow
 * additions - but it tells you early that the provider changed: a renamed field shows up as one missing
 * field (a failure) plus one new field (drift).
 */
public final class DriftCollector {

    private static final String OUTSIDE_TESTS = "(outside tests)";

    /** endpoint -> field path -> tests that saw it. */
    private static final Map<String, Map<String, Set<String>>> FIELDS = new ConcurrentHashMap<>();
    /** endpoint -> contracts that reported drift (schema paths or "openapi:<service>"). */
    private static final Map<String, Set<String>> SOURCES = new ConcurrentHashMap<>();

    private DriftCollector() {
    }

    /**
     * @param endpoint e.g. {@code GET /pet/{petId}}
     * @param source   the contract that does not declare the field: a schema path or {@code openapi:<service>}
     * @param path     JSON path with array indexes collapsed, e.g. {@code $.tags[*].color}
     */
    public static void record(String endpoint, String source, String path, String testName) {
        FIELDS.computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(path, k -> new ConcurrentSkipListSet<>())
                .add(testName == null ? OUTSIDE_TESTS : testName);
        SOURCES.computeIfAbsent(endpoint, k -> new ConcurrentSkipListSet<>()).add(source);
    }

    /** Collapses array indexes so that every element reports the same path: {@code $.a[3].b -> $.a[*].b}. */
    public static String normalize(String path) {
        return path.replaceAll("\\[\\d+]", "[*]");
    }

    public static boolean isEmpty() {
        return FIELDS.isEmpty();
    }

    public static void clear() {
        FIELDS.clear();
        SOURCES.clear();
    }

    /** endpoint -> (undeclared field path -> tests that saw it), sorted. */
    public static Map<String, Map<String, Set<String>>> snapshot() {
        Map<String, Map<String, Set<String>>> result = new TreeMap<>();
        FIELDS.forEach((endpoint, fields) -> {
            Map<String, Set<String>> copy = new TreeMap<>();
            fields.forEach((path, tests) -> copy.put(path, Set.copyOf(tests)));
            result.put(endpoint, copy);
        });
        return result;
    }

    public static String toJson() {
        Map<String, Object> report = new LinkedHashMap<>();
        snapshot().forEach((endpoint, fields) -> report.put(endpoint, Map.of(
                "reportedBy", SOURCES.getOrDefault(endpoint, Set.of()),
                "undeclaredFields", fields)));
        return Json.pretty(report);
    }

    public static String toHtml() {
        Html html = new Html("API drift: fields not declared in the contract");
        Map<String, Map<String, Set<String>>> snapshot = snapshot();
        if (snapshot.isEmpty()) {
            return html.paragraph("No drift detected: every response field is declared in its contract.").render();
        }
        html.paragraph("These fields were returned by the API but are not declared in the JSON Schema the consumer "
                + "expects and/or in the OpenAPI document. They do not fail tests. Review them: add them to the "
                + "contract, or ask the provider why they appeared.");
        List<List<String>> rows = new ArrayList<>();
        snapshot.forEach((endpoint, fields) -> fields.forEach((path, tests) -> rows.add(List.of(
                endpoint, path,
                String.join(", ", SOURCES.getOrDefault(endpoint, Set.of())),
                String.join(", ", new java.util.TreeSet<>(tests))))));
        return html.table(List.of("Endpoint", "Undeclared field", "Not declared in", "Seen in tests"), rows).render();
    }
}
