package com.testora.examples;

import com.testora.api.clients.ApiClient;
import com.testora.core.context.TestContextHolder;
import com.testora.execution.Testora;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runnable API examples against the public fake REST API https://jsonplaceholder.typicode.com.
 * Run:  mvn test -Dgroups=example
 * Needs internet access. JSONPlaceholder does not persist writes: POST/PUT/PATCH/DELETE return realistic
 * responses but nothing is stored, so a created post cannot be read back. Real projects would verify that.
 * The service has no authentication; see ApiClient.bearer(...) / apiKey(...) for secured APIs.
 */
@Testora
@Tag("example")
@Tag("api")
class ApiExampleTests {

    /** API Client Object: one class per service, with business-level methods. */
    static class PostsApi extends ApiClient {
        PostsApi() { super("https://jsonplaceholder.typicode.com"); }

        Response getPost(int id) { return get("/posts/{id}", null, id); }

        Response create(String title, String body, int userId) {
            return post("/posts", Map.of("title", title, "body", body, "userId", userId));
        }
    }

    @Test
    @Tag("smoke-api")
    void readPostAndValidateSchema() {
        Response r = new PostsApi().getPost(1);
        assertThat(r.statusCode()).isEqualTo(200);
        ApiClient.assertSchema(r, "schemas/post.json");
    }

    @Test
    void createPost() {
        Response r = new PostsApi().create("TESTORA", "created by an example test", 1);
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.jsonPath().getString("title")).isEqualTo("TESTORA");
        assertThat(r.jsonPath().getInt("id")).isPositive();
    }

    @Test
    void updateAndPatchPost() {
        PostsApi api = new PostsApi();
        Response put = api.put("/posts/{id}", Map.of("id", 1, "title", "new", "body", "b", "userId", 1), 1);
        assertThat(put.statusCode()).isEqualTo(200);
        assertThat(put.jsonPath().getString("title")).isEqualTo("new");

        Response patch = api.patch("/posts/{id}", Map.of("title", "patched"), 1);
        assertThat(patch.statusCode()).isEqualTo(200);
        assertThat(patch.jsonPath().getString("title")).isEqualTo("patched");
    }

    @Test
    void deletePost() {
        assertThat(new PostsApi().delete("/posts/{id}", 1).statusCode()).isEqualTo(200);
    }

    /** Request chaining: a value from one response feeds the next request, through the shared TestContext. */
    @Test
    @Tag("e2e")
    void chainedWorkflow() {
        PostsApi api = new PostsApi();
        Integer userId = ApiClient.extractToContext(api.getPost(1), "userId", "post.userId");

        Integer fromContext = TestContextHolder.current().<Integer>get("post.userId").orElseThrow();
        Response user = api.get("/users/{id}", null, fromContext);

        assertThat(userId).isEqualTo(fromContext);
        assertThat(user.statusCode()).isEqualTo(200);
        assertThat(user.jsonPath().getInt("id")).isEqualTo(userId);
    }

    @Test
    void negativeScenarios() {
        PostsApi api = new PostsApi();
        assertThat(api.getPost(999999).statusCode()).isEqualTo(404);
        assertThat(api.get("/does-not-exist", null).statusCode()).isEqualTo(404);
    }

    @Test
    void queryParameters() {
        Response r = new PostsApi().get("/posts", Map.of("userId", 1));
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.jsonPath().getList("userId", Integer.class)).isNotEmpty().containsOnly(1);
    }
}
