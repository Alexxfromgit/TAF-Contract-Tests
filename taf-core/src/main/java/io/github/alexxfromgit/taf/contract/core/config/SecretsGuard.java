package io.github.alexxfromgit.taf.contract.core.config;

import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fails fast when a configuration file contains a literal value for a secret-looking key.
 * Secrets belong in environment variables (or CI secret stores), never in files committed to git.
 * Placeholders such as {@code ${OTHER_KEY}} and empty values are allowed.
 */
public final class SecretsGuard {

    private static final Pattern SECRET_KEY = Pattern.compile(
            "(?i).*(password|passwd|secret|token|api[-_.]?key|private[-_.]?key|credential).*");
    private static final Pattern NON_SECRET_SUFFIX = Pattern.compile(
            "(?i).*(\\.header|\\.url|-url|\\.uri|-uri|\\.type|\\.param|\\.prefix|\\.in)$");

    private SecretsGuard() {
    }

    public static boolean looksLikeSecret(String key) {
        return SECRET_KEY.matcher(key).matches() && !NON_SECRET_SUFFIX.matcher(key).matches();
    }

    /** Returns the offending keys of one properties layer (empty when clean). */
    public static List<String> findViolations(Map<String, String> layer) {
        return layer.entrySet().stream()
                .filter(e -> looksLikeSecret(e.getKey()))
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .filter(e -> !e.getValue().trim().matches("\\$\\{[^}]+}"))
                .filter(e -> !e.getValue().trim().matches("(?i)true|false"))   // switches like lint.secrets=true
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    static void check(String source, Map<String, String> layer) {
        List<String> violations = findViolations(layer);
        if (!violations.isEmpty()) {
            throw new FrameworkException("Secret-like keys must not have values in " + source + ": " + violations
                    + ". Remove the values and provide them as environment variables, e.g. "
                    + TafConfig.toEnvName(violations.get(0)) + ". (Disable with taf.secrets-guard.enabled=false.)");
        }
    }
}
