package com.testora.examples;

import com.testora.api.clients.ApiClient;
import com.testora.config.TestoraConfig;
import com.testora.core.context.TestContextHolder;
import com.testora.execution.Testora;
import com.testora.testdata.DataGen;
import com.testora.web.elements.SmartElement;
import com.testora.web.pages.BasePage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Examples of the tester experience. They are @Disabled because they need a real application under test:
 * point base.url / api.base.url in config/qa.yaml at your AUT, adapt the locators, remove @Disabled.
 * Note: no waits, no driver handling, no screenshots, no retries anywhere in test code.
 */
@Testora
@Disabled("Requires a real application under test (see class comment)")
class ExampleTests {

    // ---- Page Object (whole page) ----
    static class LoginPage extends BasePage {
        private final SmartElement username = element(By.id("username"), "Username");
        private final SmartElement password = element(By.id("password"), "Password");
        private final SmartElement submit = element(By.id("loginButton"), "Login Button")
                .withFallback(By.cssSelector("button[type=submit]"));

        void login(String user, String pass) {
            open("/login");
            username.type(user);
            password.type(pass);
            submit.click();
        }
    }

    // ---- API client object ----
    static class CustomerApi extends ApiClient {
        CustomerApi() { super(TestoraConfig.get().string("api.base.url", "")); }

        String create(String name) {
            var response = post("/customers", Map.of("name", name));
            assertThat(response.statusCode()).isEqualTo(201);
            return ApiClient.extractToContext(response, "id", "customer.id");
        }

        void verifyDeleted(String id) { assertThat(get("/customers/{id}", null, id).statusCode()).isEqualTo(404); }
    }

    @Test @Tag("smoke") @Tag("web")
    void loginShowsDashboard() {
        new LoginPage().login("demo", TestoraConfig.get().secret("DEMO_PASSWORD"));
        SmartElement.web(By.id("dashboard"), "Dashboard").verifyVisible();
    }

    @Test @Tag("api")
    void createThenDeleteCustomerViaApi() {
        CustomerApi api = new CustomerApi();
        String id = api.create(DataGen.fromConfig().text("cust", 6));
        api.delete("/customers/{id}", id);
        api.verifyDeleted(id);
    }

    /** API -> Web -> Mobile sharing one TestContext. */
    @Test @Tag("e2e")
    void customerCreatedByApiIsVisibleOnWebAndMobile() {
        String id = new CustomerApi().create("Ahmed");
        assertThat(TestContextHolder.current().<String>get("customer.id")).contains(id);
        SmartElement.web(By.cssSelector("[data-customer-id='" + id + "']"), "Customer row").verifyVisible();
        SmartElement.mobile(io.appium.java_client.AppiumBy.accessibilityId("customer-" + id), "Customer cell").verifyVisible();
    }
}
