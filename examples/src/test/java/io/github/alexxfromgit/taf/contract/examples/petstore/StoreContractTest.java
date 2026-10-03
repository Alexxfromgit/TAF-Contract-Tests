package io.github.alexxfromgit.taf.contract.examples.petstore;

import io.github.alexxfromgit.taf.contract.core.contract.schema.ContractAssert;
import io.github.alexxfromgit.taf.contract.core.contract.schema.ExpectedSchema;
import io.github.alexxfromgit.taf.contract.core.testng.Groups;
import io.github.alexxfromgit.taf.contract.examples.petstore.model.Order;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.concurrent.ThreadLocalRandom;

import static io.github.alexxfromgit.taf.contract.examples.support.Epics.PETSTORE;
import static io.github.alexxfromgit.taf.contract.examples.support.Owners.API_TEAM;

@Epic(PETSTORE)
@Feature("Store")
@Owner(API_TEAM)
public class StoreContractTest {

    private final StoreClient store = new StoreClient();

    /**
     * The schema of this test was not written by hand: it was generated from a live response with
     * {@code -Dcontract.record=missing}, reviewed and committed (see docs/contract-testing.md, "Record mode").
     */
    @Test(groups = Groups.SMOKE, description = "Inventory is a map of status to count")
    @ExpectedSchema("schemas/petstore/recorded/inventory.json")
    public void inventory() {
        store.inventory().then().statusCode(200);
    }

    @Test(groups = Groups.CRITICAL, description = "Placed order is confirmed with all fields the client shows")
    public void placeOrder() {
        Order order = new Order(ThreadLocalRandom.current().nextLong(1, 10), 10L, 1,
                "2026-10-01T10:00:00.000+00:00", "placed", false);

        Response response = store.placeOrder(order);

        response.then().statusCode(200);
        ContractAssert.assertThat(response).strict().matchesSchema("schemas/petstore/order.json");
    }

    @Test(description = "Order details match the consumer schema")
    @ExpectedSchema("schemas/petstore/order.json")
    public void getOrder() {
        store.getOrder(5).then().statusCode(200);
    }
}
