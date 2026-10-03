package io.github.alexxfromgit.taf.contract.core.contract.coverage;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** All operations declared in an OpenAPI document. */
public final class EndpointCatalog {

    private final List<Endpoint> endpoints;

    public EndpointCatalog(List<Endpoint> endpoints) {
        this.endpoints = List.copyOf(endpoints);
    }

    public static EndpointCatalog from(OpenAPI api) {
        List<Endpoint> endpoints = new ArrayList<>();
        if (api.getPaths() != null) {
            api.getPaths().forEach((path, item) -> {
                for (Map.Entry<PathItem.HttpMethod, Operation> op : item.readOperationsMap().entrySet()) {
                    Operation operation = op.getValue();
                    String tag = operation.getTags() == null || operation.getTags().isEmpty()
                            ? "default" : operation.getTags().get(0);
                    endpoints.add(new Endpoint(op.getKey().name().toUpperCase(Locale.ROOT), PathTemplate.of(path),
                            operation.getOperationId(), tag));
                }
            });
        }
        endpoints.sort(Comparator.comparing((Endpoint e) -> e.path().template()).thenComparing(Endpoint::method));
        return new EndpointCatalog(endpoints);
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    /** The most specific declared endpoint matching a concrete call. */
    public Optional<Endpoint> match(String method, String rawPath) {
        return endpoints.stream()
                .filter(e -> e.matches(method, rawPath))
                .max(Comparator.comparingInt(e -> e.path().specificity()));
    }

    public Optional<Endpoint> find(String method, String template) {
        String normalized = PathTemplate.of(template).template();
        return endpoints.stream()
                .filter(e -> e.method().equalsIgnoreCase(method) && e.path().template().equals(normalized))
                .findFirst();
    }
}
