package io.github.alexxfromgit.taf.contract.core.failure;

/**
 * Base class for problems that are NOT product defects. Allure reports them as "broken",
 * so the report separates "the product is wrong" from "the test could not do its job".
 */
public abstract class TafRuntimeException extends RuntimeException {

    protected TafRuntimeException(String message) {
        super(message);
    }

    protected TafRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
