package io.github.alexxfromgit.taf.contract.core.testng.fixtures;

import io.github.alexxfromgit.taf.contract.core.testng.KnownIssue;
import org.testng.annotations.Test;

/** Fixture for ListenerIntegrationTest - not run directly by Surefire. */
public class KnownIssueFixture {

    @Test
    @KnownIssue(id = "BUG-1", url = "https://example.org/issues/1")
    public void failsBecauseOfKnownBug() {
        throw new AssertionError("total is wrong");
    }
}
