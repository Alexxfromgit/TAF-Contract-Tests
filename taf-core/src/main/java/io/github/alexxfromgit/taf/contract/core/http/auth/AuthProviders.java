package io.github.alexxfromgit.taf.contract.core.http.auth;

import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates (and caches, one per service) the {@link AuthProvider} described by {@code services.<id>.auth.*}.
 * Caching matters for OAuth2: all clients of a service share one token.
 */
public final class AuthProviders {

    private static final Map<String, AuthProvider> CACHE = new ConcurrentHashMap<>();

    private AuthProviders() {
    }

    public static AuthProvider forService(String serviceId) {
        return CACHE.computeIfAbsent(serviceId, AuthProviders::create);
    }

    /** Overrides the provider of a service programmatically (useful for custom flows and tests). */
    public static void register(String serviceId, AuthProvider provider) {
        CACHE.put(serviceId, provider);
    }

    public static void clear() {
        CACHE.clear();
    }

    static AuthProvider create(String serviceId) {
        TafConfig config = TafConfig.get();
        String prefix = "services." + serviceId + ".auth.";
        String type = config.string(prefix + "type", "none").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "none" -> new NoAuth();
            case "bearer" -> HeaderTokenAuth.bearer(() -> config.secret(prefix + "token"));
            case "api-key" -> new HeaderTokenAuth(
                    "query".equalsIgnoreCase(config.string(prefix + "in", "header"))
                            ? config.string(prefix + "param", "api_key")
                            : config.string(prefix + "header", "X-API-Key"),
                    config.string(prefix + "prefix", ""),
                    "query".equalsIgnoreCase(config.string(prefix + "in", "header")),
                    () -> config.secret(prefix + "token"));
            case "basic" -> new BasicAuth(config.string(prefix + "username"),
                    () -> config.secret(prefix + "password"));
            case "oauth2" -> new OAuth2ClientCredentialsAuth(
                    config.string(prefix + "token-url"),
                    config.string(prefix + "client-id"),
                    () -> config.secret(prefix + "client-secret"),
                    config.string(prefix + "scope", ""),
                    "basic".equalsIgnoreCase(config.string(prefix + "client-auth", "body")),
                    config.duration(prefix + "refresh-skew", Duration.ofSeconds(30)),
                    Clock.systemUTC());
            case "custom" -> instantiate(config.string(prefix + "class"), serviceId);
            default -> throw new FrameworkException("Unknown " + prefix + "type '" + type
                    + "'. Use none, bearer, api-key, basic, oauth2 or custom.");
        };
    }

    private static AuthProvider instantiate(String className, String serviceId) {
        try {
            Class<?> type = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            if (!AuthProvider.class.isAssignableFrom(type)) {
                throw new FrameworkException(className + " does not implement " + AuthProvider.class.getName());
            }
            try {
                return (AuthProvider) type.getConstructor(String.class).newInstance(serviceId);
            } catch (NoSuchMethodException e) {
                return (AuthProvider) type.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new FrameworkException("Cannot create custom AuthProvider " + className, e);
        }
    }
}
