package io.github.alexxfromgit.taf.contract.examples.petstore;

import io.github.alexxfromgit.taf.contract.core.contract.schema.ContractAssert;
import io.github.alexxfromgit.taf.contract.core.contract.schema.ExpectedSchema;
import io.github.alexxfromgit.taf.contract.core.log.Log;
import io.github.alexxfromgit.taf.contract.core.testng.Groups;
import io.github.alexxfromgit.taf.contract.core.verify.Verify;
import io.github.alexxfromgit.taf.contract.examples.petstore.model.Pet;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.github.alexxfromgit.taf.contract.examples.support.Epics.PETSTORE;
import static io.github.alexxfromgit.taf.contract.examples.support.Owners.API_TEAM;

/**
 * Consumer contract of the "pet" resource: the fields our client relies on must exist with the right types.
 * Every call is additionally validated against the provider's OpenAPI document (services.petstore.openapi).
 */
@Epic(PETSTORE)
@Feature("Pets")
@Owner(API_TEAM)
public class PetContractTest {

    private final PetClient pets = new PetClient();

    @Test(groups = {Groups.SMOKE, Groups.CRITICAL}, description = "Pet details match the consumer schema")
    @ExpectedSchema(value = "schemas/petstore/pet.json", endpoint = "GET /pet/{petId}")
    public void getPetById() {
        Pet pet = Pet.random("rex");
        pets.addPet(pet).then().statusCode(200);

        pets.getPet(pet.id()).then().statusCode(200);
    }

    @Test(groups = Groups.SMOKE, description = "Search by status returns a list of pets")
    @ExpectedSchema("schemas/petstore/pet-list.json")
    public void findPetsByStatus() {
        Response response = pets.findByStatus("available");

        response.then().statusCode(200);
        Verify.that(!response.jsonPath().getList("$").isEmpty(), "At least one available pet is returned");
    }

    @Test(description = "Created pet is returned exactly as sent (strict: no undeclared fields)")
    public void addPetReturnsCreatedPet() {
        Pet pet = Pet.random("bella");

        Response response = pets.addPet(pet);

        response.then().statusCode(200);
        ContractAssert.assertThat(response).strict().matchesSchema("schemas/petstore/pet.json");
        Verify.equal(response.as(Pet.class), pet, "Saved pet");
    }

    @Test(description = "Pet status can be changed")
    @ExpectedSchema(value = "schemas/petstore/pet.json", endpoint = "PUT /pet")
    public void updatePetStatus() {
        Pet pet = Pet.random("max");
        pets.addPet(pet).then().statusCode(200);

        Response response = pets.updatePet(pet.withStatus("sold"));

        response.then().statusCode(200);
        Verify.equal(response.jsonPath().getString("status"), "sold", "Status after update");
    }

    @Test(description = "Unknown pet id returns 404 (documented error response)")
    public void unknownPetReturnsNotFound() {
        pets.getPet(9_999_999_999L).then().statusCode(404);
    }

    @Test(description = "Pet can be deleted")
    public void deletePet() {
        Pet pet = Pet.random("luna");
        pets.addPet(pet).then().statusCode(200);

        pets.deletePet(pet.id()).then().statusCode(200);
        Log.info("Deleted pet {}", pet.id());
    }
}
