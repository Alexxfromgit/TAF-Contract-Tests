package io.github.alexxfromgit.taf.contract.core.testng.fixtures;

import io.github.alexxfromgit.taf.contract.core.testng.Quarantined;
import org.testng.annotations.Test;

/** Fixture for ListenerIntegrationTest - not run directly by Surefire. */
public class QuarantineFixture {

    @Test
    @Quarantined(until = "2999-01-01", reason = "fixture")
    public void quarantinedUntilFarFuture() {
        throw new IllegalStateException("must not run");
    }

    @Test
    public void notQuarantined() {
    }
}
