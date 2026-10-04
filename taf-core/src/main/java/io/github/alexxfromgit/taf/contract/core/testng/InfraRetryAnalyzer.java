package io.github.alexxfromgit.taf.contract.core.testng;

import io.github.alexxfromgit.taf.contract.core.config.TafConfig;
import io.github.alexxfromgit.taf.contract.core.log.Log;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.List;

/**
 * Retries a test only when it failed for an infrastructure reason (connection refused, timeouts,
 * {@code EnvironmentException}, ...). Assertion failures are never retried: retrying them hides real bugs.
 * <p>
 * {@code retry.max} (default 1) and {@code retry.on} (comma-separated class names, matched against
 * the whole cause chain, including subclasses).
 */
public class InfraRetryAnalyzer implements IRetryAnalyzer {

    private int attempts;

    @Override
    public boolean retry(ITestResult result) {
        TafConfig config = TafConfig.get();
        int max = config.integer("retry.max", 1);
        if (attempts >= max || !isInfrastructureFailure(result.getThrowable(), retryOn(config))) {
            return false;
        }
        attempts++;
        Log.warn("Retrying {} ({}/{}) after infrastructure failure: {}",
                result.getMethod().getMethodName(), attempts, max, String.valueOf(result.getThrowable()));
        return true;
    }

    static List<String> retryOn(TafConfig config) {
        return Arrays.stream(config.string("retry.on").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static boolean isInfrastructureFailure(Throwable throwable, List<String> classNames) {
        for (Throwable t = throwable; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof AssertionError) {
                return false;
            }
            for (Class<?> type = t.getClass(); type != null; type = type.getSuperclass()) {
                if (classNames.contains(type.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
