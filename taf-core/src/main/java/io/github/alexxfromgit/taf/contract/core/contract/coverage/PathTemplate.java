package io.github.alexxfromgit.taf.contract.core.contract.coverage;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * An OpenAPI-style path template such as {@code /pet/{petId}/uploadImage}.
 * {@link #specificity()} prefers literal segments, so {@code /pet/findByStatus} wins over {@code /pet/{petId}}.
 */
public final class PathTemplate {

    private final String template;
    private final Pattern pattern;
    private final int literalSegments;
    private final int segments;

    private PathTemplate(String template) {
        this.template = normalize(template);
        String[] parts = this.template.equals("/") ? new String[0] : this.template.substring(1).split("/", -1);
        this.segments = parts.length;
        this.literalSegments = (int) Arrays.stream(parts).filter(p -> !p.contains("{")).count();
        StringBuilder regex = new StringBuilder("^");
        for (String part : parts) {
            regex.append('/');
            regex.append(part.contains("{")
                    ? Pattern.quote(part).replaceAll("\\{[^}/]+}", "\\\\E[^/]+\\\\Q")
                    : Pattern.quote(part));
        }
        regex.append("/?$");
        this.pattern = Pattern.compile(regex.toString());
    }

    public static PathTemplate of(String template) {
        return new PathTemplate(template);
    }

    public boolean matches(String path) {
        return pattern.matcher(normalize(stripQuery(path))).matches();
    }

    /** Higher is more specific: literal segments count most, then total segments. */
    public int specificity() {
        return literalSegments * 1000 + segments;
    }

    public String template() {
        return template;
    }

    @Override
    public String toString() {
        return template;
    }

    private static String stripQuery(String path) {
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    private static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String result = path.startsWith("/") ? path : "/" + path;
        return result.length() > 1 && result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
    }
}
