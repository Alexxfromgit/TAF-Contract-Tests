package io.github.alexxfromgit.taf.contract.core.failure;

/**
 * The environment is unavailable or misbehaving: timeouts, connection refused, 5xx from a dependency
 * (Allure: broken, "Environment / infrastructure"). The only failure type that is retried by default.
 */
public class EnvironmentException extends TafRuntimeException {

    public EnvironmentException(String message) {
        super(message);
    }

    public EnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
