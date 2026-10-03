package io.github.alexxfromgit.taf.contract.examples.auth;

import io.github.alexxfromgit.taf.contract.core.contract.schema.ExpectedSchema;
import io.github.alexxfromgit.taf.contract.core.stub.EmbeddedWireMock;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.alexxfromgit.taf.contract.examples.support.Epics.SECURITY;
import static io.github.alexxfromgit.taf.contract.examples.support.Owners.PLATFORM_TEAM;

/**
 * OAuth2 client-credentials against a fake identity provider served by the embedded WireMock.
 * The client secret is NOT in any file: CI and local runs pass it as SERVICES_SECURE_API_AUTH_CLIENT_SECRET
 * (the examples POM sets a dummy system property that only the local stub accepts).
 */
@Epic(SECURITY)
@Feature("OAuth2 client credentials")
@Owner(PLATFORM_TEAM)
public class OAuth2ClientCredentialsTest {

    private final SecureApiClient api = new SecureApiClient();

    @BeforeClass
    public void requireStub() {
        if (!EmbeddedWireMock.isRunning()) {
            throw new SkipException("The OAuth2 example uses the fake identity provider of env=stub");
        }
    }

    @Test(description = "Token is acquired once and reused for subsequent calls")
    @ExpectedSchema("schemas/secure/profile.json")
    public void tokenIsAcquiredOnceAndReused() {
        api.profile().then().statusCode(200);
        api.profile().then().statusCode(200);

        EmbeddedWireMock.server().verify(1, postRequestedFor(urlPathEqualTo("/oauth/token")));
    }
}
