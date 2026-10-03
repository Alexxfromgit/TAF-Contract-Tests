package io.github.alexxfromgit.taf.contract.core.contract.coverage;

import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.report.Html;
import io.github.alexxfromgit.taf.contract.core.report.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * API coverage per service: which documented endpoints were exercised by the run.
 * Also lists calls to endpoints that the specification does NOT document - often a sign of an outdated spec.
 */
public final class CoverageReport {

    /** Coverage of one documented endpoint. */
    public record EndpointCoverage(Endpoint endpoint, Set<Integer> statuses, Set<String> declaredBy) {
        public boolean covered() {
            return !statuses.isEmpty() || !declaredBy.isEmpty();
        }
    }

    /** Coverage of one service. */
    public record ServiceCoverage(String serviceId, List<EndpointCoverage> endpoints, Set<String> undocumented) {
        public long coveredCount() {
            return endpoints.stream().filter(EndpointCoverage::covered).count();
        }

        public double percent() {
            return endpoints.isEmpty() ? 100.0 : 100.0 * coveredCount() / endpoints.size();
        }
    }

    private final List<ServiceCoverage> services;

    private CoverageReport(List<ServiceCoverage> services) {
        this.services = services;
    }

    /** Builds the report for every service that has an OpenAPI document configured. */
    public static CoverageReport build() {
        List<ServiceCoverage> services = new ArrayList<>();
        for (String serviceId : configuredServices()) {
            EndpointCatalogs.forService(serviceId).ifPresent(catalog -> services.add(build(serviceId, catalog,
                    CoverageCollector.executed(), CoverageCollector.declared())));
        }
        return new CoverageReport(services);
    }

    static ServiceCoverage build(String serviceId, EndpointCatalog catalog,
                                 Set<CoverageCollector.ExecutedCall> executed,
                                 Set<CoverageCollector.DeclaredCoverage> declared) {
        Map<String, Set<Integer>> statuses = new LinkedHashMap<>();
        Map<String, Set<String>> declaredBy = new LinkedHashMap<>();
        Set<String> undocumented = new TreeSet<>();

        for (CoverageCollector.ExecutedCall call : executed) {
            if (!call.serviceId().equals(serviceId)) {
                continue;
            }
            catalog.match(call.method(), call.path()).ifPresentOrElse(
                    e -> statuses.computeIfAbsent(e.label(), k -> new TreeSet<>()).add(call.status()),
                    () -> undocumented.add(call.method() + " " + call.path()));
        }
        for (CoverageCollector.DeclaredCoverage d : declared) {
            if (!d.serviceId().isEmpty() && !d.serviceId().equals(serviceId)) {
                continue;
            }
            catalog.find(d.method(), d.template()).ifPresent(
                    e -> declaredBy.computeIfAbsent(e.label(), k -> new TreeSet<>()).add(d.test()));
        }
        List<EndpointCoverage> endpoints = catalog.endpoints().stream()
                .map(e -> new EndpointCoverage(e, statuses.getOrDefault(e.label(), Set.of()),
                        declaredBy.getOrDefault(e.label(), Set.of())))
                .toList();
        return new ServiceCoverage(serviceId, endpoints, undocumented);
    }

    public List<ServiceCoverage> services() {
        return services;
    }

    /** Lowest coverage across services (100 when nothing is measured). */
    public double minPercent() {
        return services.stream().mapToDouble(ServiceCoverage::percent).min().orElse(100.0);
    }

    public String summary() {
        if (services.isEmpty()) {
            return "API coverage: no service has an OpenAPI document configured (services.<id>.openapi)";
        }
        StringBuilder out = new StringBuilder("API coverage:");
        for (ServiceCoverage s : services) {
            out.append(String.format("%n  %-20s %d/%d endpoints (%.1f%%)%s", s.serviceId(), s.coveredCount(),
                    s.endpoints().size(), s.percent(),
                    s.undocumented().isEmpty() ? "" : ", " + s.undocumented().size() + " undocumented call(s)"));
        }
        return out.toString();
    }

    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        for (ServiceCoverage s : services) {
            Map<String, Object> service = new LinkedHashMap<>();
            service.put("covered", s.coveredCount());
            service.put("total", s.endpoints().size());
            service.put("percent", Math.round(s.percent() * 10) / 10.0);
            Map<String, Object> endpoints = new TreeMap<>();
            s.endpoints().forEach(e -> endpoints.put(e.endpoint().label(), Map.of(
                    "covered", e.covered(), "statuses", e.statuses(), "declaredBy", e.declaredBy())));
            service.put("endpoints", endpoints);
            service.put("undocumentedCalls", s.undocumented());
            root.put(s.serviceId(), service);
        }
        return Json.pretty(root);
    }

    public String toHtml() {
        Html html = new Html("API coverage");
        if (services.isEmpty()) {
            return html.paragraph(summary()).render();
        }
        for (ServiceCoverage s : services) {
            html.heading(2, String.format("%s: %d/%d endpoints (%.1f%%)", s.serviceId(), s.coveredCount(),
                    s.endpoints().size(), s.percent()));
            List<List<String>> rows = new ArrayList<>();
            for (EndpointCoverage e : s.endpoints()) {
                rows.add(List.of(
                        e.endpoint().tag(),
                        e.endpoint().label(),
                        e.covered() ? "!good:yes" : "!bad:no",
                        e.statuses().isEmpty() ? "" : e.statuses().toString(),
                        String.join(", ", e.declaredBy())));
            }
            html.table(List.of("Tag", "Endpoint", "Covered", "Statuses seen", "Declared by @Covers"), rows);
            if (!s.undocumented().isEmpty()) {
                html.heading(2, s.serviceId() + ": calls to undocumented endpoints");
                html.table(List.of("Call"), s.undocumented().stream().map(List::of).toList());
            }
        }
        return html.render();
    }

    private static Set<String> configuredServices() {
        Set<String> ids = new TreeSet<>();
        TafConfig.get().withPrefix("services.").keySet().forEach(k -> {
            int dot = k.indexOf('.');
            if (dot > 0 && k.substring(dot + 1).equals("openapi")) {
                ids.add(k.substring(0, dot));
            }
        });
        return ids;
    }
}
