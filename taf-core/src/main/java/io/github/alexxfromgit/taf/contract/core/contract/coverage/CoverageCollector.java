package io.github.alexxfromgit.taf.contract.core.contract.coverage;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Suite-wide record of which endpoints were exercised (automatically) or declared covered ({@link Covers}). */
public final class CoverageCollector {

    /** One concrete call: {@code GET /pet/10 -> 200}. */
    public record ExecutedCall(String serviceId, String method, String path, int status) {
    }

    /** A declaration from {@link Covers}. */
    public record DeclaredCoverage(String serviceId, String method, String template, String test) {
    }

    private static final Set<ExecutedCall> EXECUTED = ConcurrentHashMap.newKeySet();
    private static final Set<DeclaredCoverage> DECLARED = ConcurrentHashMap.newKeySet();

    private CoverageCollector() {
    }

    public static void recordExecuted(String serviceId, String method, String path, int status) {
        EXECUTED.add(new ExecutedCall(serviceId, method.toUpperCase(Locale.ROOT), stripQuery(path), status));
    }

    public static void recordDeclared(Method testMethod) {
        String test = testMethod.getDeclaringClass().getSimpleName() + "." + testMethod.getName();
        for (Covers covers : testMethod.getDeclaringClass().getAnnotationsByType(Covers.class)) {
            add(covers, test);
        }
        for (Covers covers : testMethod.getAnnotationsByType(Covers.class)) {
            add(covers, test);
        }
    }

    public static Set<ExecutedCall> executed() {
        return Set.copyOf(EXECUTED);
    }

    public static Set<DeclaredCoverage> declared() {
        return Set.copyOf(DECLARED);
    }

    public static void clear() {
        EXECUTED.clear();
        DECLARED.clear();
    }

    private static void add(Covers covers, String test) {
        DECLARED.add(new DeclaredCoverage(covers.service(), covers.method().toUpperCase(Locale.ROOT),
                PathTemplate.of(covers.path()).template(), test));
    }

    private static String stripQuery(String path) {
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }
}
