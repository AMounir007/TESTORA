# 10 - API Contract Analysis (OpenAPI)

TESTORA reads an OpenAPI 3 file and (1) generates test scenarios from the contract rules, (2) runs them through `ApiClient`, and (3) reports which endpoints your tests never called. It needs no AI and no extra dependency.

## Supported and not supported

| Supported | Not supported yet |
|---|---|
| OpenAPI 3.x, JSON or YAML | Swagger 2.0 |
| Path and query parameters | Header and cookie parameters |
| JSON request-body fields: type, required, minLength, maxLength, minimum, maximum, enum | Nested object validation, `oneOf` / `anyOf` / `allOf` |
| Local `#/...` `$ref` | Remote `$ref` files or URLs |
| Security declared globally or per operation | Checking which auth scheme is required |
| Response codes listed per operation | Validating response bodies against the spec (use `ApiClient.assertSchema`) |

## Generated scenarios

Each scenario names the spec rule it comes from and expects a status **class** (`2xx` or `4xx`).

| Scenario | Expected |
|---|---|
| Valid request | 2xx |
| Each required body field missing | 4xx |
| Wrong type for each field | 4xx |
| String below minLength / above maxLength | 4xx |
| String at minLength / at maxLength | 2xx |
| Number below minimum / above maximum | 4xx |
| Number at minimum / at maximum | 2xx |
| Invalid enum value | 4xx |
| Required query parameter missing | 4xx |
| No credentials (only if security is declared) | 4xx |

The spec cannot express business rules (for example "a loan above the limit is rejected"). Write those tests yourself. A wrong or outdated spec produces wrong scenarios. Some APIs legitimately answer 404 or 409 to a valid request because of missing data, so review failures before calling them defects.

## Use it

```java
OpenApiSpec spec = OpenApiSpec.load("openapi/customers.yaml");   // classpath first, then file path
ApiClient authed    = new ApiClient(baseUrl).bearer(token);
ApiClient anonymous = new ApiClient(baseUrl);                     // null = skip "no credentials" scenarios

List<ContractRunner.Result> results = ContractRunner.runAll(authed, anonymous, spec.operations());
results.stream().filter(r -> !r.passed()).forEach(System.out::println);
```

Or generate only and inspect: `ScenarioGenerator.generate(operation)`.

Tip: run these against a test environment only. The generated requests create and modify data. Register cleanup for anything that should be removed.

## Endpoint coverage in the report

1. Set `openapi.spec` (classpath resource or file path) in `qa.yaml` or with `-Dopenapi.spec=...`.
2. Call your APIs with `ApiClient` using the spec path template, for example `delete("/customers/{id}", id)`. A path already filled in (`/customers/42`) cannot be matched to the spec.
3. `report.html` gains an **API contract coverage** section: `X of Y endpoint/method pairs called`, plus the ones never called.

Coverage means "was called", not "was well verified". It is counted per JVM run.

## Possible next step

Send the scenario list and spec to the AI gateway to suggest missing business scenarios. Not built. Such suggestions would be labelled as inference and need human approval.
