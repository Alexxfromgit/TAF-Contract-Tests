package io.github.alexxfromgit.taf.contract.core.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.contract.openapi.OpenApiValidationFilter;
import io.github.alexxfromgit.taf.contract.core.http.auth.AuthFilter;
import io.github.alexxfromgit.taf.contract.core.http.auth.AuthProviders;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for API clients. One subclass per service; configuration lives under {@code services.<id>.*}:
 * <pre>
 * services.petstore.base-uri=https://petstore3.swagger.io/api/v3
 * services.petstore.auth.type=none            # none | bearer | api-key | basic | oauth2 | custom
 * services.petstore.openapi=classpath:openapi/petstore.json   # optional: validate every exchange
 * </pre>
 * Every call gets a fresh {@link RequestSpecification} (no shared mutable state, safe for parallel runs) with:
 * authentication, Allure request/response attachments (credentials masked), call recording for
 * {@code @ExpectedSchema} and coverage, and optional OpenAPI validation.
 */
public abstract class ServiceClient {

    private static final List<String> ALWAYS_MASKED = List.of(
            "Authorization", "Proxy-Authorization", "Cookie", "Set-Cookie", "X-API-Key");

    private final String serviceId;

    protected ServiceClient(String serviceId) {
        this.serviceId = serviceId;
    }

    public String serviceId() {
        return serviceId;
    }

    /** Resolved base URI, e.g. {@code http://localhost:51234/api/v3}. */
    public String baseUri() {
        return TafConfig.get().string(key("base-uri"));
    }

    /** Starting point for every request of this client. */
    protected RequestSpecification request() {
        TafConfig config = TafConfig.get();
        String baseUri = baseUri();
        RequestSpecification spec = RestAssured.given()
                .config(restAssuredConfig(config))
                .baseUri(baseUri)
                .contentType(config.string(key("content-type"), "application/json"))
                .accept(config.string(key("accept"), "application/json"))
                .filter(new AuthFilter(AuthProviders.forService(serviceId)))
                .filter(new CallRecorderFilter(serviceId, URI.create(baseUri).getPath()))
                .filter(new AllureRestAssured());
        OpenApiValidationFilter.forService(serviceId).ifPresent(spec::filter);
        return spec;
    }

    protected String key(String suffix) {
        return "services." + serviceId + "." + suffix;
    }

    private RestAssuredConfig restAssuredConfig(TafConfig config) {
        int connectMs = (int) config.duration("http.connect-timeout").toMillis();
        int readMs = (int) config.duration("http.read-timeout").toMillis();

        List<String> masked = new ArrayList<>(ALWAYS_MASKED);
        config.optional("http.masked-headers").ifPresent(v ->
                Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(masked::add));
        config.optional(key("auth.header")).ifPresent(masked::add);

        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", connectMs)
                        .setParam("http.socket.timeout", readMs))
                .logConfig(LogConfig.logConfig()
                        .blacklistHeaders(masked)
                        .enableLoggingOfRequestAndResponseIfValidationFails())
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> new ObjectMapper()
                                .findAndRegisterModules()
                                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)));
    }
}
