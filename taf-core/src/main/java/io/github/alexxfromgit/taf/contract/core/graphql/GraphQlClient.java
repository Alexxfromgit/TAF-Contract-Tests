package io.github.alexxfromgit.taf.contract.core.graphql;

import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.github.alexxfromgit.taf.contract.core.http.ServiceClient;
import io.qameta.allure.Allure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GraphQL over HTTP POST. Documents live in {@code src/test/resources/graphql/**.graphql}, so queries are
 * readable, diffable and reusable. Configuration: {@code services.<id>.base-uri} (+ optional
 * {@code services.<id>.graphql-path}, default empty) and the usual {@code auth.*} keys.
 * <pre>{@code
 * GraphQlResponse r = countries.execute("graphql/countries/country.graphql", Map.of("code", "UA"));
 * r.assertNoErrors();
 * }</pre>
 * Combine with {@code @ExpectedSchema(value = "...", jsonPointer = "/data")} to contract-test the payload.
 */
public class GraphQlClient extends ServiceClient {

    private static final Pattern OPERATION = Pattern.compile("^\\s*(query|mutation|subscription)\\s+(\\w+)",
            Pattern.MULTILINE);
    private static final Map<String, String> DOCUMENTS = new ConcurrentHashMap<>();

    public GraphQlClient(String serviceId) {
        super(serviceId);
    }

    /** Executes a document from the classpath with variables. The operation name is taken from the document. */
    public GraphQlResponse execute(String documentPath, Map<String, ?> variables) {
        String document = DOCUMENTS.computeIfAbsent(documentPath, GraphQlClient::load);
        return executeInline(document, variables);
    }

    public GraphQlResponse executeInline(String document, Map<String, ?> variables) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", document);
        String operationName = operationName(document);
        if (operationName != null) {
            payload.put("operationName", operationName);
        }
        payload.put("variables", variables == null ? Map.of() : variables);
        String stepName = "GraphQL " + (operationName == null ? "anonymous operation" : operationName)
                + (variables == null || variables.isEmpty() ? "" : " " + variables);
        return Allure.step(stepName, () -> new GraphQlResponse(
                request().body(payload).post(config("graphql-path", ""))));
    }

    static String operationName(String document) {
        Matcher matcher = OPERATION.matcher(document);
        return matcher.find() ? matcher.group(2) : null;
    }

    private String config(String suffix, String defaultValue) {
        return TafConfig.get().string(key(suffix), defaultValue);
    }

    private static String load(String path) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new FrameworkException("GraphQL document not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FrameworkException("Cannot read GraphQL document " + path, e);
        }
    }
}
