package io.github.alexxfromgit.taf.contract.core.failure;

/**
 * The product behaved differently from what the test expects (Allure: failed, "Product defects").
 * Extends {@link AssertionError} so that TestNG and Allure treat it as a test failure, not an error.
 */
public class PotentialDefectException extends AssertionError {

    public PotentialDefectException(String message) {
        super(message);
    }

    public PotentialDefectException(String message, Throwable cause) {
        super(message, cause);
    }
}
