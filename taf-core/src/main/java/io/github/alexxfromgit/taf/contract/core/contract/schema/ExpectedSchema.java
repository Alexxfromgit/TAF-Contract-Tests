package io.github.alexxfromgit.taf.contract.core.contract.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the JSON Schema that a response of this test must satisfy. After the test body finishes
 * successfully, the framework picks the recorded HTTP call and validates its body - the test itself
 * contains only the business steps.
 * <pre>{@code
 * @Test
 * @ExpectedSchema("schemas/petstore/pet.json")
 * public void getPetById() { pets.getPet(10).then().statusCode(200); }
 * }</pre>
 * By default the schema is <b>consumer-tolerant</b>: required fields and types are enforced, extra fields
 * are allowed but reported by the drift report. Use {@code strict = true} to reject undeclared fields.
 * Equivalent fluent API: {@link ContractAssert}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ExpectedSchemas.class)
public @interface ExpectedSchema {

    /** Classpath location of the schema, e.g. {@code "schemas/petstore/pet.json"}. */
    String value();

    /** Reject properties that the schema does not declare. */
    boolean strict() default false;

    /**
     * Which call to validate, as {@code "METHOD /path/template"}, e.g. {@code "GET /pet/{petId}"}.
     * Empty: the last call of the test that returned JSON.
     */
    String endpoint() default "";

    /** Validate only a part of the body, e.g. {@code "/data"} for GraphQL. Empty: the whole body. */
    String jsonPointer() default "";
}
