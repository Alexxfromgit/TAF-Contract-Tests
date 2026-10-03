package io.github.alexxfromgit.taf.contract.core.contract.schema;

/**
 * One schema violation in a readable, flattened form.
 *
 * @param path     JSON path of the offending value, e.g. {@code $.tags[0]}
 * @param keyword  failed schema keyword, e.g. {@code required}, {@code type}, {@code additionalProperties}
 * @param message  validator message
 * @param property property name the violation is about (for {@code required}/{@code additionalProperties}), or null
 */
public record Violation(String path, String keyword, String message, String property) {

    /** Path including the property, e.g. {@code $.tags[0].name}. */
    public String fullPath() {
        if (property == null || property.isEmpty()) {
            return path;
        }
        return path + (property.matches("[A-Za-z_$][A-Za-z0-9_$]*") ? "." + property : "['" + property + "']");
    }

    @Override
    public String toString() {
        return fullPath() + " -> " + keyword + ": " + message;
    }
}
