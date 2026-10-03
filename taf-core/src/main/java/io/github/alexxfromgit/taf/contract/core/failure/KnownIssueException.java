package io.github.alexxfromgit.taf.contract.core.failure;

/**
 * Wraps a failure of a test annotated with {@code @KnownIssue}, so it lands in the
 * "Known issues" Allure category instead of looking like a new defect.
 */
public class KnownIssueException extends AssertionError {

    public KnownIssueException(String issueId, Throwable cause) {
        super("[Known issue " + issueId + "] " + cause.getMessage(), cause);
    }
}
