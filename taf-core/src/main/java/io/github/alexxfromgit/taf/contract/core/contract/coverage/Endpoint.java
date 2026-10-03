package io.github.alexxfromgit.taf.contract.core.contract.coverage;

/** One operation of an API specification, e.g. {@code GET /pet/{petId}}. */
public record Endpoint(String method, PathTemplate path, String operationId, String tag) {

    public String label() {
        return method + " " + path.template();
    }

    public boolean matches(String httpMethod, String rawPath) {
        return method.equalsIgnoreCase(httpMethod) && path.matches(rawPath);
    }
}
