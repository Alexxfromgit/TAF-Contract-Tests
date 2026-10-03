package io.github.alexxfromgit.taf.contract.core.contract.schema;

import io.github.alexxfromgit.taf.contract.core.contract.coverage.PathTemplate;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.github.alexxfromgit.taf.contract.core.http.CallRecord;
import io.github.alexxfromgit.taf.contract.core.testng.TestContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Executes the {@link ExpectedSchema} checks of a finished test method against its recorded calls. */
public final class ExpectedSchemaVerifier {

    private ExpectedSchemaVerifier() {
    }

    public static boolean hasExpectations(Method method) {
        return method.getAnnotationsByType(ExpectedSchema.class).length > 0;
    }

    public static void verify(Method method, TestContext context) {
        for (ExpectedSchema expectation : method.getAnnotationsByType(ExpectedSchema.class)) {
            CallRecord call = select(expectation.endpoint(), context.calls(), method);
            ContractAssert.assertThat(call)
                    .at(expectation.jsonPointer())
                    .strict(expectation.strict())
                    .matchesSchema(expectation.value());
        }
    }

    static CallRecord select(String endpoint, List<CallRecord> calls, Method method) {
        List<CallRecord> candidates;
        if (endpoint.isBlank()) {
            candidates = calls.stream().filter(CallRecord::isJson).toList();
        } else {
            String[] parts = endpoint.trim().split("\\s+", 2);
            if (parts.length != 2) {
                throw new FrameworkException("@ExpectedSchema(endpoint = \"" + endpoint
                        + "\") must look like \"GET /pet/{petId}\"");
            }
            String httpMethod = parts[0].toUpperCase(Locale.ROOT);
            PathTemplate template = PathTemplate.of(parts[1]);
            candidates = calls.stream()
                    .filter(c -> c.method().equals(httpMethod) && template.matches(c.path()))
                    .toList();
        }
        if (candidates.isEmpty()) {
            throw new FrameworkException("@ExpectedSchema on " + method.getName() + " found no matching "
                    + (endpoint.isBlank() ? "JSON response" : "call to " + endpoint) + ". Recorded calls: "
                    + calls.stream().map(c -> c.label() + " -> " + c.status()).collect(Collectors.joining(", ", "[", "]"))
                    + ". Only calls made through a ServiceClient are recorded.");
        }
        return candidates.get(candidates.size() - 1);
    }
}
