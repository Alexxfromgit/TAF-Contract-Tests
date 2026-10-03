package io.github.alexxfromgit.taf.contract.examples.petstore;

import io.github.alexxfromgit.taf.contract.core.http.ServiceClient;
import io.github.alexxfromgit.taf.contract.examples.petstore.model.User;
import io.qameta.allure.Step;
import io.restassured.response.Response;

public class UserClient extends ServiceClient {

    public UserClient() {
        super("petstore");
    }

    @Step("Get user '{username}'")
    public Response getUser(String username) {
        return request().pathParam("username", username).get("/user/{username}");
    }

    @Step("Create user '{user.username}'")
    public Response createUser(User user) {
        return request().body(user).post("/user");
    }
}
