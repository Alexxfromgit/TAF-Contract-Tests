package io.github.alexxfromgit.taf.contract.core.http;

import io.github.alexxfromgit.taf.contract.core.contract.coverage.CoverageCollector;
import io.github.alexxfromgit.taf.contract.core.testng.TestContext;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.net.URI;

/**
 * Records every exchange: into the current {@link TestContext} (used by {@code @ExpectedSchema})
 * and into the suite-wide {@link CoverageCollector} (used by the API coverage report).
 */
public class CallRecorderFilter implements OrderedFilter {

    private final String serviceId;
    private final String basePath;

    public CallRecorderFilter(String serviceId, String basePath) {
        this.serviceId = serviceId;
        this.basePath = normalizeBasePath(basePath);
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
                           FilterContext context) {
        long start = System.nanoTime();
        Response result = context.next(request, response);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        String path = relativePath(URI.create(request.getURI()).getRawPath());
        String method = request.getMethod().toUpperCase();
        CallRecord call = new CallRecord(serviceId, method, path, result.statusCode(), durationMs,
                result.contentType(), result.asString());
        TestContext.current().recordCall(call);
        CoverageCollector.recordExecuted(serviceId, method, path, result.statusCode());
        return result;
    }

    @Override
    public int getOrder() {
        return OrderedFilter.DEFAULT_PRECEDENCE;
    }

    private String relativePath(String fullPath) {
        String path = fullPath == null || fullPath.isEmpty() ? "/" : fullPath;
        if (!basePath.isEmpty() && path.startsWith(basePath)) {
            path = path.substring(basePath.length());
        }
        return path.isEmpty() ? "/" : path;
    }

    static String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isBlank() || basePath.equals("/")) {
            return "";
        }
        String trimmed = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
