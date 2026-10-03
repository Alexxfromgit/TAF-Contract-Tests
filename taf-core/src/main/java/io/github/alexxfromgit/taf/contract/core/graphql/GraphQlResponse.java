package io.github.alexxfromgit.taf.contract.core.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.alexxfromgit.taf.contract.core.failure.ContractViolationException;
import io.github.alexxfromgit.taf.contract.core.failure.EnvironmentException;
import io.github.alexxfromgit.taf.contract.core.failure.PotentialDefectException;
import io.github.alexxfromgit.taf.contract.core.report.Json;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * A GraphQL response. GraphQL usually answers HTTP 200 even when the operation failed, so always check
 * {@link #assertNoErrors()} (or assert on {@link #errors()} for negative tests).
 */
public final class GraphQlResponse {

    private final Response raw;
    private final JsonNode body;

    GraphQlResponse(Response raw) {
        this.raw = raw;
        if (raw.statusCode() >= 500) {
            throw new EnvironmentException("GraphQL endpoint returned HTTP " + raw.statusCode());
        }
        this.body = Json.tryParse(raw.asString()).orElseThrow(() -> new ContractViolationException(
                "GraphQL response is not JSON (HTTP " + raw.statusCode() + "): " + raw.asString()));
    }

    public GraphQlResponse assertNoErrors() {
        List<String> errors = errors();
        if (!errors.isEmpty()) {
            throw new PotentialDefectException("GraphQL operation returned " + errors.size() + " error(s):\n  "
                    + String.join("\n  ", errors));
        }
        return this;
    }

    /** Error messages with their paths, e.g. {@code "Country not found (path: country)"}. */
    public List<String> errors() {
        List<String> messages = new ArrayList<>();
        JsonNode errors = body.path("errors");
        errors.forEach(e -> {
            String path = e.has("path") ? " (path: " + joinPath(e.get("path")) + ")" : "";
            messages.add(e.path("message").asText("<no message>") + path);
        });
        return messages;
    }

    public JsonNode data() {
        return body.path("data");
    }

    public JsonNode body() {
        return body;
    }

    public Response raw() {
        return raw;
    }

    private static String joinPath(JsonNode path) {
        List<String> parts = new ArrayList<>();
        path.forEach(p -> parts.add(p.asText()));
        return String.join(".", parts);
    }
}
