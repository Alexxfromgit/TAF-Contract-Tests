package io.github.alexxfromgit.taf.contract.core.contract.coverage;

import io.github.alexxfromgit.taf.contract.core.contract.openapi.OpenApiSpecs;
import io.github.alexxfromgit.taf.contract.core.http.CallRecord;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Per-service {@link EndpointCatalog}, built from the service's OpenAPI document when one is configured. */
public final class EndpointCatalogs {

    private static final Map<String, Optional<EndpointCatalog>> CACHE = new ConcurrentHashMap<>();

    private EndpointCatalogs() {
    }

    public static Optional<EndpointCatalog> forService(String serviceId) {
        return CACHE.computeIfAbsent(serviceId,
                id -> OpenApiSpecs.forService(id).map(spec -> EndpointCatalog.from(spec.model())));
    }

    /** {@code GET /pet/{petId}} when the call matches a documented endpoint, otherwise {@code GET /pet/10}. */
    public static String label(CallRecord call) {
        return forService(call.serviceId())
                .flatMap(catalog -> catalog.match(call.method(), call.path()))
                .map(Endpoint::label)
                .orElse(call.label());
    }
}
