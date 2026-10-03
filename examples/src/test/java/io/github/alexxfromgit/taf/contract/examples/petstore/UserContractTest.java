package io.github.alexxfromgit.taf.contract.examples.petstore;

import io.github.alexxfromgit.taf.contract.core.contract.schema.ExpectedSchema;
import io.github.alexxfromgit.taf.contract.examples.petstore.model.User;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.testng.annotations.Test;

import java.util.concurrent.ThreadLocalRandom;

import static io.github.alexxfromgit.taf.contract.examples.support.Epics.PETSTORE;
import static io.github.alexxfromgit.taf.contract.examples.support.Owners.API_TEAM;

@Epic(PETSTORE)
@Feature("Users")
@Owner(API_TEAM)
public class UserContractTest {

    private final UserClient users = new UserClient();

    @Test(description = "User profile matches the consumer schema (draft-07 schema)")
    @ExpectedSchema(value = "schemas/petstore/user.json", endpoint = "GET /user/{username}")
    public void getUser() {
        String username = "taf-" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        users.createUser(new User(1L, username, "Contract", "Tester", username + "@example.org", "555-0100", 1))
                .then().statusCode(200);

        users.getUser(username).then().statusCode(200);
    }
}
