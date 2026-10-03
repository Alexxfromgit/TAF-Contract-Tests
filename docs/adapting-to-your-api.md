# Adapting the template to your API

## 1. Create your repository

Click **Use this template** on GitHub (or clone and re-initialise git), then check that it builds:

```bash
./mvnw verify
```

## 2. Rename the package and coordinates (optional but recommended)

- `pom.xml` files: `groupId` (`io.github.alexxfromgit`) and the project name/URL.
- Java packages `io.github.alexxfromgit.taf.contract.*`: use your IDE's *Refactor > Rename package* on
  `taf-core` and `examples`.
- `taf-core/src/main/resources/META-INF/services/org.testng.ITestNGListener`: update the class names.
- `taf-core/src/main/resources/taf/defaults.properties`: update the `retry.on` entry for `EnvironmentException`.
- Suite files (`examples/src/test/resources/suites/*.xml`): package and class names.

## 3. Describe your service

`examples/src/test/resources/taf.properties`:

```properties
services.orders.openapi=classpath:openapi/orders.json      # optional, enables validation + coverage
services.orders.auth.type=oauth2
services.orders.auth.client-id=contract-tests
services.orders.auth.token-url=https://idp.example.com/oauth/token
```

`examples/src/test/resources/env/staging.properties`:

```properties
services.orders.base-uri=https://staging.example.com/orders/v1
```

The secret goes into an environment variable: `SERVICES_ORDERS_AUTH_CLIENT_SECRET`.

## 4. Write a client

```java
public class OrdersClient extends ServiceClient {
    public OrdersClient() { super("orders"); }

    @Step("Get order {id}")
    public Response getOrder(String id) {
        return request().pathParam("id", id).get("/orders/{id}");
    }
}
```

## 5. Write a contract test

```java
@Epic("Orders API") @Feature("Orders") @Owner("checkout-team")
public class OrdersContractTest {
    private final OrdersClient orders = new OrdersClient();

    @Test(groups = Groups.SMOKE)
    @ExpectedSchema("schemas/orders/order.json")
    public void getOrder() {
        orders.getOrder("A-1").then().statusCode(200);
    }
}
```

No schema yet? Run once with `-Dcontract.record=missing -Denv=staging`, review the generated file and commit it.

## 6. Offline stubs (optional)

Put WireMock mappings in `src/test/resources/wiremock/mappings/` and an `env/stub.properties` with
`stub.enabled=true` and `services.orders.base-uri=${stub.base-url}/orders/v1`. CI can then run without your
environments. You can also record real traffic with WireMock's recorder and keep the mappings.

## 7. Remove the examples

Delete the Petstore and Countries clients, tests, schemas, stubs and `openapi/petstore-openapi.json`, then update
the package lists in the suites. Keep `lint/MetadataLintTest` and the `ContractReports` block.

## 8. CI

`.github/workflows/ci.yml` builds and publishes the Allure report to GitHub Pages. Enable it in the repository's
*Settings > Pages > Source: GitHub Actions*. Add secrets in *Settings > Secrets and variables > Actions* and map
them to environment variables in the workflow:

```yaml
      - run: ./mvnw -B -ntp verify -Denv=staging
        env:
          SERVICES_ORDERS_AUTH_CLIENT_SECRET: ${{ secrets.ORDERS_CLIENT_SECRET }}
```
