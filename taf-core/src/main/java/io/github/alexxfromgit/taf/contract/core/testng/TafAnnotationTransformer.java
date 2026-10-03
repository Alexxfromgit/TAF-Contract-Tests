package io.github.alexxfromgit.taf.contract.core.testng;

import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Applied to every {@code @Test} before the run:
 * <ul>
 *     <li>{@link Quarantined} tests are disabled until their date;</li>
 *     <li>tests without an explicit retry analyzer get {@link InfraRetryAnalyzer}.</li>
 * </ul>
 * Registered through {@code META-INF/services/org.testng.ITestNGListener}.
 */
public class TafAnnotationTransformer implements IAnnotationTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(TafAnnotationTransformer.class);

    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        Quarantined quarantined = findQuarantine(testClass, testMethod);
        if (quarantined != null && isActive(quarantined, LocalDate.now())) {
            annotation.setEnabled(false);
            LOG.warn("Quarantined until {}: {} ({})", quarantined.until(), describe(testClass, testMethod),
                    quarantined.reason());
        }
        Class<?> current = annotation.getRetryAnalyzerClass();
        if (current == null || current.getName().equals("org.testng.internal.annotations.DisabledRetryAnalyzer")) {
            annotation.setRetryAnalyzer(InfraRetryAnalyzer.class);
        }
    }

    static boolean isActive(Quarantined quarantined, LocalDate today) {
        try {
            return today.isBefore(LocalDate.parse(quarantined.until()));
        } catch (DateTimeParseException e) {
            throw new FrameworkException("@Quarantined(until = \"" + quarantined.until()
                    + "\") must be an ISO date like 2026-11-01", e);
        }
    }

    private static Quarantined findQuarantine(Class<?> testClass, Method testMethod) {
        if (testMethod != null && testMethod.isAnnotationPresent(Quarantined.class)) {
            return testMethod.getAnnotation(Quarantined.class);
        }
        Class<?> type = testClass != null ? testClass : testMethod != null ? testMethod.getDeclaringClass() : null;
        return type == null ? null : type.getAnnotation(Quarantined.class);
    }

    private static String describe(Class<?> testClass, Method testMethod) {
        return testMethod != null
                ? testMethod.getDeclaringClass().getSimpleName() + "." + testMethod.getName()
                : String.valueOf(testClass);
    }
}
