package io.github.alexxfromgit.taf.contract.core.failure;

/**
 * A response does not satisfy its contract: JSON Schema or OpenAPI specification
 * (Allure: failed, "Contract violations").
 */
public class ContractViolationException extends AssertionError {

    public ContractViolationException(String message) {
        super(message);
    }
}
