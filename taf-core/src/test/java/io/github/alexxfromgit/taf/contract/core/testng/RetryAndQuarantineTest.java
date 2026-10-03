package io.github.alexxfromgit.taf.contract.core.testng;

import io.github.alexxfromgit.taf.contract.core.failure.EnvironmentException;
import io.github.alexxfromgit.taf.contract.core.failure.FrameworkException;
import io.github.alexxfromgit.taf.contract.core.failure.PotentialDefectException;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.net.ConnectException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RetryAndQuarantineTest {

    private static final List<String> RETRY_ON = List.of(
            EnvironmentException.class.getName(), ConnectException.class.getName());

    @Test
    public void infrastructureFailuresAreRetried() {
        assertThat(InfraRetryAnalyzer.isInfrastructureFailure(new EnvironmentException("down"), RETRY_ON)).isTrue();
        assertThat(InfraRetryAnalyzer.isInfrastructureFailure(
                new RuntimeException("wrapped", new ConnectException("refused")), RETRY_ON)).isTrue();
    }

    @Test
    public void assertionsAndBugsAreNeverRetried() {
        assertThat(InfraRetryAnalyzer.isInfrastructureFailure(new PotentialDefectException("wrong"), RETRY_ON)).isFalse();
        assertThat(InfraRetryAnalyzer.isInfrastructureFailure(new AssertionError("wrong"), RETRY_ON)).isFalse();
        assertThat(InfraRetryAnalyzer.isInfrastructureFailure(new FrameworkException("bug"), RETRY_ON)).isFalse();
    }

    @Test
    public void quarantineIsActiveUntilItsDate() {
        LocalDate today = LocalDate.of(2026, 10, 1);
        assertThat(TafAnnotationTransformer.isActive(quarantine("2026-10-02"), today)).isTrue();
        assertThat(TafAnnotationTransformer.isActive(quarantine("2026-10-01"), today)).isFalse();
        assertThat(TafAnnotationTransformer.isActive(quarantine("2026-09-01"), today)).isFalse();
        assertThatThrownBy(() -> TafAnnotationTransformer.isActive(quarantine("next week"), today))
                .isInstanceOf(FrameworkException.class);
    }

    private static Quarantined quarantine(String until) {
        return new Quarantined() {
            @Override
            public String until() {
                return until;
            }

            @Override
            public String reason() {
                return "test";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Quarantined.class;
            }
        };
    }
}
