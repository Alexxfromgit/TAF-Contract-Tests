package io.github.alexxfromgit.taf.contract.core.contract.openapi;

import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the OpenAPI document of a service from {@code services.<id>.openapi}:
 * {@code classpath:openapi/petstore.json}, an {@code http(s)://} URL or a file path.
 * Bundling a snapshot in the repository makes contract checks reproducible; pointing to the live URL
 * checks against what the provider publishes right now.
 */
public final class OpenApiSpecs {

    private static final Map<String, Optional<Spec>> CACHE = new ConcurrentHashMap<>();

    /** Raw document text plus its parsed model. */
    public record Spec(String location, String content, OpenAPI model) {
    }

    private OpenApiSpecs() {
    }

    public static Optional<Spec> forService(String serviceId) {
        return CACHE.computeIfAbsent(serviceId, id ->
                TafConfig.get().optional("services." + id + ".openapi").map(OpenApiSpecs::load));
    }

    static Spec load(String location) {
        String content = read(location);
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        if (result.getOpenAPI() == null) {
            throw new FrameworkException("Cannot parse OpenAPI document " + location + ": " + result.getMessages());
        }
        return new Spec(location, content, result.getOpenAPI());
    }

    private static String read(String location) {
        try {
            if (location.startsWith("classpath:")) {
                String resource = location.substring("classpath:".length()).replaceFirst("^/", "");
                try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                    if (in == null) {
                        throw new FrameworkException("OpenAPI document not found on classpath: " + resource);
                    }
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            if (location.startsWith("http://") || location.startsWith("https://")) {
                try (InputStream in = java.net.URI.create(location).toURL().openStream()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            return Files.readString(Path.of(location), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FrameworkException("Cannot read OpenAPI document " + location, e);
        }
    }
}
