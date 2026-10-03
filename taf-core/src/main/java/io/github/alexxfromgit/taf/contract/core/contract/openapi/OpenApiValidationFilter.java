package io.github.alexxfromgit.taf.contract.core.contract.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.restassured.RestAssuredRequest;
import com.atlassian.oai.validator.restassured.RestAssuredResponse;
import io.github.alexxfromgit.taf.contract.core.allure.AllureResults;
import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.contract.drift.DriftCollector;
import io.github.alexxfromgit.taf.contract.core.failure.ContractViolationException;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.github.alexxfromgit.taf.contract.core.testng.TestContext;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates every request and response of a service against its OpenAPI document (Atlassian
 * openapi-request-validator). Enabled when {@code services.<id>.openapi} is set; disable per service with
 * {@code services.<id>.openapi.validate=false}.
 * <p>
 * Message levels follow the validator's keys and can be tuned in configuration:
 * <pre>
 * contract.openapi.level.validation.response.body.missing=IGNORE
 * contract.openapi.level.validation.request.parameter.query.unexpected=WARN
 * </pre>
 * ERROR messages fail the call with {@link ContractViolationException}; WARN/INFO messages are attached to Allure.
 * <p>
 * Limitation: OpenAPI 3.1 support of the underlying validator is partial - prefer 3.0.x documents.
 */
public final class OpenApiValidationFilter implements OrderedFilter {

    private static final Map<String, Optional<OpenApiValidationFilter>> CACHE = new ConcurrentHashMap<>();
    private static final Pattern PROPERTY = Pattern.compile("property '([^']+)'");

    private final String serviceId;
    private final OpenApiInteractionValidator validator;

    private OpenApiValidationFilter(String serviceId, OpenApiInteractionValidator validator) {
        this.serviceId = serviceId;
        this.validator = validator;
    }

    public static Optional<OpenApiValidationFilter> forService(String serviceId) {
        return CACHE.computeIfAbsent(serviceId, id -> {
            TafConfig config = TafConfig.get();
            if (!config.bool("services." + id + ".openapi.validate", true)) {
                return Optional.empty();
            }
            return OpenApiSpecs.forService(id).map(spec -> new OpenApiValidationFilter(id,
                    OpenApiInteractionValidator.createForInlineApiSpecification(spec.content())
                            .withLevelResolver(levelResolver(config))
                            .build()));
        });
    }

    static LevelResolver levelResolver(TafConfig config) {
        LevelResolver.Builder builder = LevelResolver.create();
        config.withPrefix("contract.openapi.level.").forEach((key, level) -> {
            try {
                builder.withLevel(key, ValidationReport.Level.valueOf(level.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new FrameworkException("contract.openapi.level." + key + " must be ERROR, WARN, INFO or IGNORE");
            }
        });
        return builder.build();
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
                           FilterContext context) {
        Response result = context.next(request, response);
        ValidationReport report = validator.validate(RestAssuredRequest.of(request), RestAssuredResponse.of(result));

        List<ValidationReport.Message> errors = byLevel(report, ValidationReport.Level.ERROR);
        List<ValidationReport.Message> notes = report.getMessages().stream()
                .filter(m -> m.getLevel() == ValidationReport.Level.WARN || m.getLevel() == ValidationReport.Level.INFO)
                .toList();
        if (!notes.isEmpty()) {
            AllureResults.attachText("OpenAPI warnings", format(notes));
            notes.stream().filter(m -> m.getKey().endsWith("response.body.schema.additionalProperties"))
                    .forEach(m -> recordDrift(request, m));
        }
        if (!errors.isEmpty()) {
            AllureResults.attachText("OpenAPI violations", format(errors));
            throw new ContractViolationException(request.getMethod() + " " + request.getURI()
                    + " does not match the OpenAPI document of '" + serviceId + "':\n" + format(errors));
        }
        return result;
    }

    /** Outermost filter: validates after Allure has attached the exchange. */
    @Override
    public int getOrder() {
        return OrderedFilter.HIGHEST_PRECEDENCE;
    }

    /** Undeclared response fields found by the OpenAPI check go to the same drift report as schema drift. */
    private void recordDrift(FilterableRequestSpecification request, ValidationReport.Message message) {
        Matcher property = PROPERTY.matcher(message.getMessage());
        if (!property.find()) {
            return;
        }
        Optional<ValidationReport.MessageContext> context = message.getContext();
        String pointer = context.flatMap(ValidationReport.MessageContext::getPointers)
                .map(ValidationReport.MessageContext.Pointers::getInstance).orElse("");
        String endpoint = request.getMethod().toUpperCase(Locale.ROOT) + " " + context
                .flatMap(ValidationReport.MessageContext::getApiOperation)
                .map(op -> op.getApiPath().original())
                .orElse(java.net.URI.create(request.getURI()).getPath());
        String test = TestContext.current().method()
                .map(m -> m.getDeclaringClass().getSimpleName() + "." + m.getName()).orElse(null);
        DriftCollector.record(endpoint, "openapi:" + serviceId,
                DriftCollector.normalize(toJsonPath(pointer) + "." + property.group(1)), test);
    }

    /** {@code /tags/0} -> {@code $.tags[0]}. */
    static String toJsonPath(String pointer) {
        StringBuilder path = new StringBuilder("$");
        for (String part : pointer.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            String unescaped = part.replace("~1", "/").replace("~0", "~");
            path.append(unescaped.matches("\\d+") ? "[" + unescaped + "]" : "." + unescaped);
        }
        return path.toString();
    }

    private static List<ValidationReport.Message> byLevel(ValidationReport report, ValidationReport.Level level) {
        return report.getMessages().stream().filter(m -> m.getLevel() == level).toList();
    }

    private static String format(List<ValidationReport.Message> messages) {
        return messages.stream()
                .map(m -> "  [" + m.getKey() + "] " + m.getMessage()
                        + (m.getAdditionalInfo().isEmpty() ? "" : " " + m.getAdditionalInfo()))
                .collect(Collectors.joining("\n"));
    }
}
