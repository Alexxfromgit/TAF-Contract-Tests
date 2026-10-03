package io.github.alexxfromgit.taf.contract.core.failure;

/**
 * The test could not find or create the data it needs (Allure: broken, "Test data problems").
 */
public class TestDataException extends TafRuntimeException {

    public TestDataException(String message) {
        super(message);
    }

    public TestDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
