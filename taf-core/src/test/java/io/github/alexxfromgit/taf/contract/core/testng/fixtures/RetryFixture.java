package io.github.alexxfromgit.taf.contract.core.testng.fixtures;

import io.github.alexxfromgit.taf.contract.core.failure.EnvironmentException;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

/** Fixture for ListenerIntegrationTest - not run directly by Surefire. */
public class RetryFixture {

    private static final AtomicInteger FLAKY = new AtomicInteger();
    private static final AtomicInteger ASSERTION = new AtomicInteger();

    public static void reset() {
        FLAKY.set(0);
        ASSERTION.set(0);
    }

    public static int flakyInvocations() {
        return FLAKY.get();
    }

    public static int assertionInvocations() {
        return ASSERTION.get();
    }

    @Test
    public void passesOnSecondAttempt() {
        if (FLAKY.incrementAndGet() == 1) {
            throw new EnvironmentException("service temporarily unavailable");
        }
    }

    @Test
    public void assertionFailureIsNotRetried() {
        ASSERTION.incrementAndGet();
        throw new AssertionError("real failure");
    }
}
