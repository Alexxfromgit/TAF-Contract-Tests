package io.github.alexxfromgit.taf.contract.core.testng;

import io.github.alexxfromgit.taf.contract.core.http.CallRecord;
import org.assertj.core.api.SoftAssertions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Per-invocation state (one test or configuration method on one thread). Reset by
 * {@link TafInvocationListener} before every invocation, which keeps the framework safe
 * for {@code parallel="methods"}: nothing test-specific lives in static or instance fields.
 */
public final class TestContext {

    private static final ThreadLocal<TestContext> CURRENT = ThreadLocal.withInitial(() -> new TestContext(null));

    private final Method method;
    private final List<CallRecord> calls = Collections.synchronizedList(new ArrayList<>());
    private SoftAssertions softAssertions;

    private TestContext(Method method) {
        this.method = method;
    }

    public static TestContext current() {
        return CURRENT.get();
    }

    static TestContext start(Method method) {
        TestContext context = new TestContext(method);
        CURRENT.set(context);
        return context;
    }

    static void clear() {
        CURRENT.remove();
    }

    /** The running test method, if any (empty outside TestNG invocations). */
    public Optional<Method> method() {
        return Optional.ofNullable(method);
    }

    public void recordCall(CallRecord call) {
        calls.add(call);
    }

    /** HTTP calls made during this invocation, in order. */
    public List<CallRecord> calls() {
        synchronized (calls) {
            return List.copyOf(calls);
        }
    }

    public Optional<CallRecord> lastCall() {
        synchronized (calls) {
            return calls.isEmpty() ? Optional.empty() : Optional.of(calls.get(calls.size() - 1));
        }
    }

    /** Soft assertions collected during this invocation; verified after the test body finishes. */
    public SoftAssertions softAssertions() {
        if (softAssertions == null) {
            softAssertions = new SoftAssertions();
        }
        return softAssertions;
    }

    Optional<SoftAssertions> softAssertionsIfUsed() {
        return Optional.ofNullable(softAssertions);
    }
}
