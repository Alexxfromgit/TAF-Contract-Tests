package io.github.alexxfromgit.taf.contract.examples.petstore;

import io.github.alexxfromgit.taf.contract.core.contract.schema.ExpectedSchema;
import io.github.alexxfromgit.taf.contract.core.stub.EmbeddedWireMock;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static io.github.alexxfromgit.taf.contract.examples.support.Epics.PETSTORE;
import static io.github.alexxfromgit.taf.contract.examples.support.Owners.API_TEAM;

/**
 * Demonstrates drift detection. The stub for pet 77 returns fields that the consumer schema does not declare
 * ({@code nickname}, {@code tags[].color}). The test passes - consumer-tolerant contracts allow additions -
 * but the "API drift" report (Allure, Framework reports) lists the new fields.
 */
@Epic(PETSTORE)
@Feature("Pets")
@Owner(API_TEAM)
public class PetDriftDemoTest {

    private final PetClient pets = new PetClient();

    @Test(description = "Provider added fields: test passes, drift report lists them")
    @ExpectedSchema("schemas/petstore/pet.json")
    public void petWithUndeclaredFields() {
        if (!EmbeddedWireMock.isRunning()) {
            throw new SkipException("Drift demo relies on the bundled stub (env=stub)");
        }
        pets.getPet(77).then().statusCode(200);
    }
}
