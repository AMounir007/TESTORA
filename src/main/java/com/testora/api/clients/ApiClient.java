package com.testora.api.clients;

import com.testora.core.context.TestContextHolder;
import com.testora.core.events.EventType;
import com.testora.core.events.Events;
import com.testora.core.utilities.Masker;
import com.testora.evidence.EvidenceService;
import com.testora.observability.Metrics;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reusable API client. Subclass per service (CustomerApi extends ApiClient) and expose business verbs.
 * Authentication is configured once; every call records masked evidence and metrics.
 * Stateless apart from auth settings, so one instance per test/thread is safe.
 */
public class ApiClient {
    private final String baseUri;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private ContentType contentType = ContentType.JSON;

    public ApiClient(String baseUri) { this.baseUri = baseUri; }

    public ApiClient bearer(String token) { headers.put("Authorization", "Bearer " + token); return this; }

    public ApiClient apiKey(String header, String value) { headers.put(header, value); return this; }

    public ApiClient header(String name, String value) { headers.put(name, value); return this; }

    public ApiClient contentType(ContentType type) { this.contentType = type; return this; }

    /** OAuth2 client-credentials; credentials come from environment variables, never from code. */
    public static String clientCredentialsToken(String tokenUrl, String clientId, String clientSecret) {
        return RestAssured.given().auth().preemptive().basic(clientId, clientSecret)
                .formParam("grant_type", "client_credentials")
                .post(tokenUrl).then().statusCode(200).extract().path("access_token");
    }

    public Response get(String path, Map<String, ?> query, Object... pathParams) {
        return send(Method.GET, path, null, query, pathParams);
    }

    public Response post(String path, Object body, Object... pathParams) { return send(Method.POST, path, body, null, pathParams); }

    public Response put(String path, Object body, Object... pathParams) { return send(Method.PUT, path, body, null, pathParams); }

    public Response patch(String path, Object body, Object... pathParams) { return send(Method.PATCH, path, body, null, pathParams); }

    public Response delete(String path, Object... pathParams) { return send(Method.DELETE, path, null, null, pathParams); }

    public Response upload(String path, String controlName, File file) {
        RequestSpecification spec = base().contentType(ContentType.MULTIPART).multiPart(controlName, file);
        return record(Method.POST, path, spec.request(Method.POST, path));
    }

    /** Asserts the JSON response against a JSON schema on the classpath. */
    public static void assertSchema(Response response, String classpathSchema) {
        response.then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(classpathSchema));
    }

    /** Request chaining: extract a JSONPath value from a response and store it in the TestContext. */
    public static <T> T extractToContext(Response response, String jsonPath, String key) {
        T value = response.jsonPath().get(jsonPath);
        TestContextHolder.current().put(key, value);
        return value;
    }

    private RequestSpecification base() {
        RequestSpecification spec = RestAssured.given().baseUri(baseUri).contentType(contentType).accept(contentType);
        headers.forEach((k, v) -> spec.header(k, v));
        return spec;
    }

    private Response send(Method method, String path, Object body, Map<String, ?> query, Object... pathParams) {
        RequestSpecification spec = base();
        if (query != null) spec.queryParams(query);
        if (body != null) spec.body(body);
        return record(method, path, spec.request(method, path, pathParams));
    }

    private Response record(Method method, String path, Response response) {
        long ms = response.time();
        Metrics.recordApi(ms);
        Events.emit(EventType.ACTION_COMPLETED, "operation", "API " + method + " " + path,
                "status", response.statusCode(), "durationMs", ms);
        Map<String, String> requestHeaders = new LinkedHashMap<>(headers);
        EvidenceService.saveText("api-" + method + "-" + System.nanoTime(),
                method + " " + baseUri + path + "\nRequest headers: " + Masker.maskHeaders(requestHeaders)
                        + "\nStatus: " + response.statusCode() + "\nTime: " + ms + " ms\nResponse body:\n"
                        + response.asString());
        return response;
    }
}
