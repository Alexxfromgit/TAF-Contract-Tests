package io.github.alexxfromgit.taf.contract.core.http;

/**
 * One HTTP exchange made through a {@link ServiceClient}.
 *
 * @param serviceId   logical service name from configuration ({@code services.<id>.*})
 * @param method      HTTP method, upper case
 * @param path        request path relative to the service base path, e.g. {@code /pet/10}
 * @param status      response status code
 * @param durationMs  wall-clock time of the exchange
 * @param contentType response content type (may be empty)
 * @param body        response body as text (may be empty)
 */
public record CallRecord(String serviceId, String method, String path, int status, long durationMs,
                         String contentType, String body) {

    public boolean isJson() {
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    /** Human-readable label such as {@code GET /pet/10}. */
    public String label() {
        return method + " " + path;
    }
}
