package io.github.alexxfromgit.taf.contract.core.testng;

import io.github.alexxfromgit.taf.contract.core.failure.ContractViolationException;
import io.github.alexxfromgit.taf.contract.core.failure.KnownIssueException;
import io.github.alexxfromgit.taf.contract.core.testng.fixtures.ExpectedSchemaFixture;
import io.github.alexxfromgit.taf.contract.core.testng.fixtures.KnownIssueFixture;
import io.github.alexxfromgit.taf.contract.core.testng.fixtures.QuarantineFixture;
import io.github.alexxfromgit.taf.contract.core.testng.fixtures.RetryFixture;
import io.github.alexxfromgit.taf.contract.core.testng.fixtures.SoftAssertFixture;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs small fixture classes through a nested TestNG to prove that the listeners registered via
 * {@code META-INF/services} (including the annotation transformer) are really applied.
 */
public class ListenerIntegrationTest {

    private final List<ITestResult> results = new ArrayList<>();

    @BeforeClass
    public void runFixtures() throws InterruptedException {
        RetryFixture.reset();
        AtomicReference<TestListenerAdapter> adapter = new AtomicReference<>();
        // own thread: keeps the nested run's Allure context away from this test's context
        Thread runner = new Thread(() -> {
            TestNG testng = new TestNG(false);
            testng.setTestClasses(new Class<?>[]{QuarantineFixture.class, SoftAssertFixture.class,
                    KnownIssueFixture.class, RetryFixture.class, ExpectedSchemaFixture.class});
            TestListenerAdapter listener = new TestListenerAdapter();
            testng.addListener(listener);
            testng.run();
            adapter.set(listener);
        });
        runner.start();
        runner.join();
        TestListenerAdapter listener = adapter.get();
        results.addAll(listener.getPassedTests());
        results.addAll(listener.getFailedTests());
        results.addAll(listener.getSkippedTests());
    }

    @Test
    public void quarantinedTestIsNotRun() {
        assertThat(find("quarantinedUntilFarFuture")).isEmpty();
        assertThat(status("notQuarantined")).isEqualTo(ITestResult.SUCCESS);
    }

    @Test
    public void softAssertionsFailTheTestAfterTheBody() {
        ITestResult result = find("collectsTwoSoftFailures").orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ITestResult.FAILURE);
        assertThat(result.getThrowable()).hasMessageContaining("first").hasMessageContaining("second");
    }

    @Test
    public void knownIssueFailureIsWrapped() {
        ITestResult result = find("failsBecauseOfKnownBug").orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ITestResult.FAILURE);
        assertThat(result.getThrowable()).isInstanceOf(KnownIssueException.class)
                .hasMessageContaining("[Known issue BUG-1]");
    }

    @Test
    public void onlyInfrastructureFailuresAreRetried() {
        assertThat(RetryFixture.flakyInvocations()).isEqualTo(2);
        assertThat(lastStatus("passesOnSecondAttempt")).isEqualTo(ITestResult.SUCCESS);
        assertThat(RetryFixture.assertionInvocations()).isEqualTo(1);
        assertThat(status("assertionFailureIsNotRetried")).isEqualTo(ITestResult.FAILURE);
    }

    @Test
    public void expectedSchemaIsVerifiedAfterTheBody() {
        assertThat(status("matchingResponse")).isEqualTo(ITestResult.SUCCESS);
        ITestResult broken = find("responseMissingRequiredField").orElseThrow();
        assertThat(broken.getStatus()).isEqualTo(ITestResult.FAILURE);
        assertThat(broken.getThrowable()).isInstanceOf(ContractViolationException.class)
                .hasMessageContaining("$.name -> required");
    }

    private int status(String method) {
        return find(method).orElseThrow(() -> new AssertionError(method + " did not run")).getStatus();
    }

    private int lastStatus(String method) {
        return results.stream().filter(r -> r.getMethod().getMethodName().equals(method))
                .max((a, b) -> Long.compare(a.getEndMillis(), b.getEndMillis()))
                .orElseThrow().getStatus();
    }

    private Optional<ITestResult> find(String method) {
        return results.stream().filter(r -> r.getMethod().getMethodName().equals(method)).findFirst();
    }
}
