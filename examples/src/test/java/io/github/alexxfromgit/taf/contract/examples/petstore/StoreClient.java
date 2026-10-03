package io.github.alexxfromgit.taf.contract.examples.petstore;

import io.github.alexxfromgit.taf.contract.core.http.ServiceClient;
import io.github.alexxfromgit.taf.contract.examples.petstore.model.Order;
import io.qameta.allure.Step;
import io.restassured.response.Response;

public class StoreClient extends ServiceClient {

    public StoreClient() {
        super("petstore");
    }

    @Step("Get store inventory")
    public Response inventory() {
        return request().get("/store/inventory");
    }

    @Step("Place order for pet {order.petId}")
    public Response placeOrder(Order order) {
        return request().body(order).post("/store/order");
    }

    @Step("Get order {id}")
    public Response getOrder(long id) {
        return request().pathParam("orderId", id).get("/store/order/{orderId}");
    }
}
