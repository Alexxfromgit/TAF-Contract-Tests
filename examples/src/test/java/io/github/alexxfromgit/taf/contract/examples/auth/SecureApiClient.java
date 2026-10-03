package io.github.alexxfromgit.taf.contract.examples.auth;

import io.github.alexxfromgit.taf.contract.core.http.ServiceClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/**
 * Client of an OAuth2-protected API. Nothing auth-specific in the code: {@code services.secure-api.auth.*}
 * selects the client-credentials flow, the token is fetched, cached and attached automatically.
 */
public class SecureApiClient extends ServiceClient {

    public SecureApiClient() {
        super("secure-api");
    }

    @Step("Get own profile")
    public Response profile() {
        return request().get("/profile");
    }
}
