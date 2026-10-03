package io.github.alexxfromgit.taf.contract.core.lint;

import io.github.alexxfromgit.taf.contract.core.config.SecretsGuard;
import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.github.alexxfromgit.taf.contract.core.testng.Quarantined;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Static checks that keep a growing suite navigable and safe to publish:
 * <ul>
 *     <li>every test has an owner ({@code @Owner}) and a place in the feature map ({@code @Epic/@Feature/@Story});</li>
 *     <li>no quarantine is longer than {@code lint.quarantine.max-days} (default 90) or has a malformed date;</li>
 *     <li>no {@code .properties} file in the project contains a secret-like value.</li>
 * </ul>
 * Run it as the first test of a suite:
 * <pre>{@code MetadataLinter.forPackages("com.example.tests").assertClean();}</pre>
 * Toggle rules with {@code lint.require-owner}, {@code lint.require-feature}, {@code lint.secrets}.
 */
public final class MetadataLinter {

    private final String[] packages;

    private MetadataLinter(String... packages) {
        this.packages = packages;
    }

    public static MetadataLinter forPackages(String... packages) {
        return new MetadataLinter(packages);
    }

    public List<LintViolation> check() {
        TafConfig config = TafConfig.get();
        List<LintViolation> violations = new ArrayList<>();
        try (ScanResult scan = new ClassGraph().enableAllInfo().acceptPackages(packages).scan()) {
            for (ClassInfo info : scan.getAllClasses()) {
                if (info.isAbstract() || info.isInterface() || info.isAnnotation()) {
                    continue;
                }
                Class<?> type = info.loadClass();
                for (Method method : testMethods(type)) {
                    checkMethod(type, method, config, violations);
                }
            }
        }
        if (config.bool("lint.secrets", true)) {
            // separate scan: project directories only (no jars), all paths - taf.properties lives at the root
            try (ScanResult resources = new ClassGraph().disableJarScanning().scan()) {
                checkProperties(resources, violations);
            }
        }
        return violations;
    }

    /** Throws {@link FrameworkException} listing every violation. */
    public void assertClean() {
        List<LintViolation> violations = check();
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Metadata lint found ")
                    .append(violations.size()).append(" problem(s):");
            violations.forEach(v -> message.append("\n  ").append(v));
            throw new FrameworkException(message.toString());
        }
    }

    private static void checkMethod(Class<?> type, Method method, TafConfig config, List<LintViolation> out) {
        String where = type.getSimpleName() + "." + method.getName();
        if (config.bool("lint.require-owner", true) && !has(type, method, Owner.class)) {
            out.add(new LintViolation(where, "missing @Owner (who maintains this test?)"));
        }
        if (config.bool("lint.require-feature", true)
                && !has(type, method, Epic.class) && !has(type, method, Feature.class) && !has(type, method, Story.class)) {
            out.add(new LintViolation(where, "missing @Epic/@Feature/@Story (where does it belong in the report?)"));
        }
        Quarantined quarantined = method.isAnnotationPresent(Quarantined.class)
                ? method.getAnnotation(Quarantined.class) : type.getAnnotation(Quarantined.class);
        if (quarantined != null) {
            int maxDays = config.integer("lint.quarantine.max-days", 90);
            try {
                LocalDate until = LocalDate.parse(quarantined.until());
                if (until.isAfter(LocalDate.now().plusDays(maxDays))) {
                    out.add(new LintViolation(where, "@Quarantined until " + until + " is more than " + maxDays
                            + " days away - fix or delete the test instead"));
                }
            } catch (DateTimeParseException e) {
                out.add(new LintViolation(where, "@Quarantined(until = \"" + quarantined.until()
                        + "\") is not an ISO date (yyyy-MM-dd)"));
            }
            if (quarantined.reason().isBlank()) {
                out.add(new LintViolation(where, "@Quarantined needs a reason"));
            }
        }
    }

    private static void checkProperties(ScanResult scan, List<LintViolation> out) {
        for (Resource resource : scan.getResourcesWithExtension("properties")) {
            if (resource.getClasspathElementFile() == null || !resource.getClasspathElementFile().isDirectory()) {
                continue; // only the project's own files, not dependencies
            }
            Properties properties = new Properties();
            try (InputStream in = resource.open()) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                out.add(new LintViolation(resource.getPath(), "cannot be read: " + e.getMessage()));
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            properties.stringPropertyNames().forEach(k -> values.put(k, properties.getProperty(k)));
            SecretsGuard.findViolations(values).forEach(key -> out.add(new LintViolation(resource.getPath(),
                    "'" + key + "' looks like a secret - provide it as environment variable "
                            + TafConfig.toEnvName(key))));
        }
    }

    private static List<Method> testMethods(Class<?> type) {
        boolean classLevelTest = type.isAnnotationPresent(Test.class);
        return Arrays.stream(type.getMethods())
                .filter(m -> m.getDeclaringClass() != Object.class)
                .filter(m -> m.isAnnotationPresent(Test.class)
                        || (classLevelTest && m.getDeclaringClass() == type && !isConfiguration(m)))
                .toList();
    }

    private static boolean isConfiguration(Method method) {
        return Arrays.stream(method.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(t -> t.getPackageName().equals("org.testng.annotations") && t != Test.class);
    }

    private static boolean has(Class<?> type, Method method, Class<? extends Annotation> annotation) {
        return method.isAnnotationPresent(annotation) || type.isAnnotationPresent(annotation);
    }
}
