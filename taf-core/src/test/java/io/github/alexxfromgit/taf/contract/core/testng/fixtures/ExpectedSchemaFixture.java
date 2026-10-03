package io.github.alexxfromgit.taf.contract.core.testng.fixtures;

import io.github.alexxfromgit.taf.contract.core.contract.schema.ExpectedSchema;
import io.github.alexxfromgit.taf.contract.core.http.CallRecord;
import io.github.alexxfromgit.taf.contract.core.testng.TestContext;
import org.testng.annotations.Test;

/** Fixture for ListenerIntegrationTest - not run directly by Surefire. */
public class ExpectedSchemaFixture {

    @Test
    @ExpectedSchema("schemas/unit/pet.json")
    public void matchingResponse() {
        record("{\"id\": 1, \"name\": \"Rex\"}");
    }

    @Test
    @ExpectedSchema(value = "schemas/unit/pet.json", endpoint = "GET /pet/{petId}")
    public void responseMissingRequiredField() {
        record("{\"id\": 1}");
    }

    private static void record(String body) {
        TestContext.current().recordCall(
                new CallRecord("unit", "GET", "/pet/1", 200, 5, "application/json", body));
    }
}
