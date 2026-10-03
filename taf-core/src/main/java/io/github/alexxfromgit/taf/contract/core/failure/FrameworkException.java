package io.github.alexxfromgit.taf.contract.core.failure;

/**
 * A bug or misconfiguration in the test framework or the test code itself (Allure: broken, "Framework problems").
 */
public class FrameworkException extends TafRuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
