package io.github.alexxfromgit.taf.contract.core.config;

import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layered, read-only configuration. Later layers win:
 * <ol>
 *     <li>{@code taf/defaults.properties} - framework defaults shipped in taf-core</li>
 *     <li>{@code taf.properties} - project defaults</li>
 *     <li>{@code env/<env>.properties} - selected with {@code -Denv=...} or {@code TAF_ENV}</li>
 *     <li>System properties</li>
 *     <li>Environment variables ({@code services.petstore.base-uri} -> {@code SERVICES_PETSTORE_BASE_URI})</li>
 *     <li>{@link RuntimeOverrides} - values only known at runtime, e.g. a random stub port</li>
 * </ol>
 * Values may reference other keys with {@code ${key}} placeholders, resolved on every lookup.
 * Secrets are never read from files: use {@link #secret(String)}.
 */
public final class TafConfig {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private static final int MAX_PLACEHOLDER_DEPTH = 10;

    private static volatile TafConfig instance;

    private final Map<String, String> values;
    private final Map<String, String> environment;
    private final Map<String, String> systemProperties;
    private final String env;

    private TafConfig(Map<String, String> values, Map<String, String> environment,
                      Map<String, String> systemProperties, String env) {
        this.values = Collections.unmodifiableMap(values);
        this.environment = environment;
        this.systemProperties = systemProperties;
        this.env = env;
    }

    /** The process-wide configuration, loaded on first use. */
    public static TafConfig get() {
        TafConfig local = instance;
        if (local == null) {
            synchronized (TafConfig.class) {
                local = instance;
                if (local == null) {
                    local = load(asMap(System.getProperties()), System.getenv(),
                            Thread.currentThread().getContextClassLoader());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Drops the cached configuration so that the next {@link #get()} reloads it. Intended for tests. */
    public static synchronized void reset() {
        instance = null;
    }

    /** Builds a configuration from explicit inputs. Used by {@link #get()} and by unit tests. */
    public static TafConfig load(Map<String, String> systemProperties, Map<String, String> environment,
                                 ClassLoader classLoader) {
        Map<String, String> merged = new LinkedHashMap<>();
        Map<String, Map<String, String>> fileLayers = new LinkedHashMap<>();

        loadInto(merged, fileLayers, classLoader, "taf/defaults.properties", false);
        loadInto(merged, fileLayers, classLoader, "taf.properties", false);

        String envName = firstNonBlank(systemProperties.get("env"), environment.get("TAF_ENV"), merged.get("env"));
        if (envName != null) {
            loadInto(merged, fileLayers, classLoader, "env/" + envName + ".properties", true);
            merged.put("env", envName);
        }

        if (Boolean.parseBoolean(merged.getOrDefault("taf.secrets-guard.enabled", "true"))) {
            fileLayers.forEach(SecretsGuard::check);
        }

        String[] fileKeys = merged.keySet().toArray(String[]::new);
        merged.putAll(systemProperties);
        // env vars override only keys declared in files, so e.g. JAVA_HOME never shadows java.home
        for (String key : fileKeys) {
            String fromEnv = environment.get(toEnvName(key));
            if (fromEnv != null) {
                merged.put(key, fromEnv);
            }
        }
        return new TafConfig(merged, environment, systemProperties, envName == null ? "default" : envName);
    }

    /** Name of the active environment ({@code default} when none is selected). */
    public String env() {
        return env;
    }

    /** Required value. Throws {@link FrameworkException} when the key is missing. */
    public String string(String key) {
        return optional(key).orElseThrow(() -> new FrameworkException(
                "Missing configuration key '" + key + "' (env=" + env + "). Define it in taf.properties, "
                        + "env/" + env + ".properties, -D" + key + "=... or " + toEnvName(key)));
    }

    public String string(String key, String defaultValue) {
        return optional(key).orElse(defaultValue);
    }

    public Optional<String> optional(String key) {
        String raw = RuntimeOverrides.get(key).orElse(values.get(key));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(resolve(raw, 0).trim());
    }

    public int integer(String key) {
        return parse(key, Integer::parseInt);
    }

    public int integer(String key, int defaultValue) {
        return has(key) ? integer(key) : defaultValue;
    }

    public boolean bool(String key) {
        return parse(key, Boolean::parseBoolean);
    }

    public boolean bool(String key, boolean defaultValue) {
        return has(key) ? bool(key) : defaultValue;
    }

    /** Accepts ISO-8601 ({@code PT10S}) or short forms: {@code 500ms}, {@code 10s}, {@code 2m}. */
    public Duration duration(String key) {
        return parse(key, TafConfig::parseDuration);
    }

    public Duration duration(String key, Duration defaultValue) {
        return has(key) ? duration(key) : defaultValue;
    }

    public boolean has(String key) {
        return optional(key).isPresent();
    }

    /**
     * All keys starting with {@code prefix}, with the prefix removed and placeholders resolved.
     * {@code withPrefix("services.petstore.")} -> {@code {base-uri=..., auth.type=...}}.
     */
    public Map<String, String> withPrefix(String prefix) {
        Map<String, String> result = new TreeMap<>();
        values.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .forEach(k -> optional(k).ifPresent(v -> result.put(k.substring(prefix.length()), v)));
        RuntimeOverrides.snapshot().forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), resolve(v, 0));
            }
        });
        return result;
    }

    /**
     * Reads a secret from an environment variable (preferred) or a system property, never from files.
     * {@code secret("services.api.auth.token")} reads {@code SERVICES_API_AUTH_TOKEN}.
     */
    public String secret(String key) {
        String value = firstNonBlank(environment.get(toEnvName(key)), systemProperties.get(key));
        if (value == null) {
            throw new FrameworkException("Missing secret '" + key + "'. Provide it as environment variable "
                    + toEnvName(key) + " (or -D" + key + "=... for local runs). Secrets are never read from files.");
        }
        return value;
    }

    public Optional<String> optionalSecret(String key) {
        return Optional.ofNullable(firstNonBlank(environment.get(toEnvName(key)), systemProperties.get(key)));
    }

    /** {@code services.petstore.base-uri} -> {@code SERVICES_PETSTORE_BASE_URI}. */
    public static String toEnvName(String key) {
        return key.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
    }

    private String resolve(String value, int depth) {
        if (depth > MAX_PLACEHOLDER_DEPTH) {
            throw new FrameworkException("Placeholder recursion is too deep in value '" + value + "'");
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = RuntimeOverrides.get(key).orElse(values.get(key));
            if (replacement == null) {
                throw new FrameworkException("Unresolved placeholder ${" + key + "} in value '" + value + "'");
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(resolve(replacement, depth + 1)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private <T> T parse(String key, java.util.function.Function<String, T> parser) {
        String raw = string(key);
        try {
            return parser.apply(raw);
        } catch (RuntimeException e) {
            throw new FrameworkException("Configuration key '" + key + "' has invalid value '" + raw + "'", e);
        }
    }

    static Duration parseDuration(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("p")) {
            return Duration.parse(raw.trim().toUpperCase(Locale.ROOT));
        }
        if (value.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2).trim()));
        }
        long amount = Long.parseLong(value.substring(0, value.length() - 1).trim());
        return switch (value.charAt(value.length() - 1)) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            default -> throw new IllegalArgumentException("Unknown duration unit in '" + raw + "'");
        };
    }

    private static void loadInto(Map<String, String> target, Map<String, Map<String, String>> layers,
                                 ClassLoader classLoader, String resource, boolean required) {
        try (InputStream in = classLoader.getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new FrameworkException("Environment file '" + resource + "' was not found on the classpath");
                }
                return;
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            Map<String, String> layer = asMap(properties);
            layers.put(resource, layer);
            target.putAll(layer);
        } catch (IOException e) {
            throw new FrameworkException("Cannot read configuration file '" + resource + "'", e);
        }
    }

    private static Map<String, String> asMap(Properties properties) {
        Map<String, String> map = new LinkedHashMap<>();
        properties.stringPropertyNames().forEach(name -> map.put(name, properties.getProperty(name)));
        return map;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
