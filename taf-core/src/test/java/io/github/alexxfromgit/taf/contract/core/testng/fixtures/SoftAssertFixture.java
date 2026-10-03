package io.github.alexxfromgit.taf.contract.core.testng.fixtures;

import io.github.alexxfromgit.taf.contract.core.verify.Verify;
import org.testng.annotations.Test;

/** Fixture for ListenerIntegrationTest - not run directly by Surefire. */
public class SoftAssertFixture {

    @Test
    public void collectsTwoSoftFailures() {
        Verify.softly().assertThat(1).as("first").isEqualTo(2);
        Verify.softly().assertThat("a").as("second").isEqualTo("b");
    }
}
