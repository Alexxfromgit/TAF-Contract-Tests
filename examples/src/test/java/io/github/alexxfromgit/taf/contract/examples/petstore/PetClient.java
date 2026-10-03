package io.github.alexxfromgit.taf.contract.examples.petstore;

import io.github.alexxfromgit.taf.contract.core.http.ServiceClient;
import io.github.alexxfromgit.taf.contract.examples.petstore.model.Pet;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/**
 * API client for the "pet" resource. Methods return the raw {@link Response}: contract tests care about
 * status codes and payload shape, not only about deserialized objects.
 */
public class PetClient extends ServiceClient {

    public PetClient() {
        super("petstore");
    }

    @Step("Get pet {id}")
    public Response getPet(long id) {
        return request().pathParam("petId", id).get("/pet/{petId}");
    }

    @Step("Find pets by status '{status}'")
    public Response findByStatus(String status) {
        return request().queryParam("status", status).get("/pet/findByStatus");
    }

    @Step("Add pet '{pet.name}'")
    public Response addPet(Pet pet) {
        return request().body(pet).post("/pet");
    }

    @Step("Update pet '{pet.name}'")
    public Response updatePet(Pet pet) {
        return request().body(pet).put("/pet");
    }

    @Step("Delete pet {id}")
    public Response deletePet(long id) {
        return request().pathParam("petId", id).delete("/pet/{petId}");
    }
}
