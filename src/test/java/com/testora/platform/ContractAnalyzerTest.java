package com.testora.platform;

import com.testora.api.contracts.ApiCoverage;
import com.testora.api.contracts.OpenApiSpec;
import com.testora.api.contracts.ScenarioGenerator;
import com.testora.api.contracts.ScenarioGenerator.Scenario;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
class ContractAnalyzerTest {

    private static final String SPEC = """
            openapi: 3.0.3
            paths:
              /customers:
                post:
                  security:
                    - bearer: []
                  requestBody:
                    content:
                      application/json:
                        schema:
                          $ref: '#/components/schemas/Customer'
                  responses:
                    '201': { description: created }
                    '400': { description: invalid }
              /customers/{id}:
                get:
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema: { type: integer }
                  responses:
                    '200': { description: ok }
            components:
              schemas:
                Customer:
                  type: object
                  required: [name]
                  properties:
                    name: { type: string, minLength: 2, maxLength: 10 }
                    age: { type: integer, minimum: 18, maximum: 99 }
                    status: { type: string, enum: [ACTIVE, BLOCKED] }
            """;

    private static Scenario find(List<Scenario> all, String name) {
        return all.stream().filter(s -> s.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No scenario named: " + name));
    }

    @Test
    void readsEndpointsFieldsAndSecurity() {
        OpenApiSpec spec = OpenApiSpec.parse(SPEC);
        assertThat(spec.operations()).extracting(OpenApiSpec.Operation::key)
                .containsExactlyInAnyOrder("POST /customers", "GET /customers/{id}");
        var post = spec.operations().stream().filter(o -> o.method().equals("POST")).findFirst().orElseThrow();
        assertThat(post.secured()).isTrue();
        assertThat(post.responseCodes()).containsExactly("201", "400");
        assertThat(post.in("body")).extracting(OpenApiSpec.Field::name).containsExactly("name", "age", "status");
    }

    @Test
    void generatesScenariosFromContractRules() {
        var post = OpenApiSpec.parse(SPEC).operations().stream().filter(o -> o.method().equals("POST")).findFirst().orElseThrow();
        List<Scenario> all = ScenarioGenerator.generate(post);

        assertThat(find(all, "valid request").expected()).isEqualTo("2xx");
        assertThat(find(all, "valid request").body()).containsEntry("name", "xx").containsEntry("status", "ACTIVE");
        assertThat(find(all, "missing required field 'name'").body()).doesNotContainKey("name");
        assertThat(find(all, "'name' below minLength").body().get("name")).isEqualTo("x");
        assertThat(find(all, "'name' above maxLength").expected()).isEqualTo("4xx");
        assertThat(find(all, "'age' at maximum").body()).containsEntry("age", 99L);
        assertThat(find(all, "'age' below minimum").body()).containsEntry("age", 17L);
        assertThat(find(all, "invalid enum value for 'status'").expected()).isEqualTo("4xx");
        assertThat(find(all, "no credentials").omitAuth()).isTrue();
        assertThat(all).allSatisfy(s -> assertThat(s.specRule()).isNotBlank());
    }

    @Test
    void reportsEndpointCoverage() {
        OpenApiSpec spec = OpenApiSpec.parse(SPEC);
        ApiCoverage.record("post", "/customers?source=test");
        var coverage = ApiCoverage.compute(spec.operations());
        assertThat(coverage.covered()).contains("POST /customers");
        assertThat(coverage.missing()).contains("GET /customers/{id}");
    }
}
