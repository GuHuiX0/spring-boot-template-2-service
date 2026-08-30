# Task 5 Report: Checked-in OpenAPI Manifests and Swagger UI

## Delivered

- Added `service-a/src/main/resources/static/openapi/service-a.yaml`, an OpenAPI 3.0.3 public contract for `/api/products` and `/api/products/{id}`.
  - Documents `createProduct`, `listProducts`, `getProduct`, `updateProduct`, and `deleteProduct`.
  - Documents the implemented 201/200/204 successes, the create `Location` path header, validation and not-found errors, and Service A's upstream 502 errors.
- Added `service-b/src/main/resources/static/openapi/service-b.yaml`, an OpenAPI 3.0.3 internal contract for `/internal/products` and `/internal/products/{id}`.
  - Clearly identifies the API as internal.
  - Documents the corresponding success, validation, and not-found behavior, with no 502 responses.
- Added reusable `ProductRequest`, `ProductResponse`, and `ApiError` schemas to both manifests.
  - Product request constraints align with the Java validation annotations: required `name`, `price`, and `stockQuantity`; name length 1 through 120; nullable description with maximum length 1000; non-negative double price in 0.01 increments; and non-negative integer stock quantity.
  - Product responses include the positive `int64` ID and `date-time` timestamps.
  - Error responses require all six Java `ApiError` fields and describe `fieldErrors` as a string-valued map.
- Added Swagger UI configuration to both applications. The UI is exposed at `/swagger-ui.html` and reads each service's static `/openapi/service-*.yaml`; runtime `/v3/api-docs` generation is disabled.
- Added test-scope `org.yaml:snakeyaml`, using the Spring Boot-managed version, and Java contract tests that load the classpath manifest and assert paths, verbs, operation IDs, core schemas, and the Service A/Service B 502 distinction.

## Contract source review

Read the committed controllers, product request/response DTOs, exception handlers, Service A Feign client, Service B service, and both original `application.yml` files before writing the manifests.

- Both controllers implement only collection `GET`/`POST` and item `GET`/`PUT`/`DELETE`; neither declares query parameters.
- Both DTOs have the same fields and constraints.
- Service B maps validation to 400 and missing products to 404.
- Service A relays Service B 400/404 results and maps unavailable or unexpected upstream failures to 502.
- The original Service A Feign URL/timeouts and Service B datasource/Flyway/JPA keys were retained unchanged.

## Verification evidence

1. **TDD RED command recorded before creating the manifests**

   ```text
   mvn -pl service-a,service-b -Dtest=OpenApiContractTest test
   ```

   Result: unavailable. PowerShell reports `mvn` is not recognized. `java -version` subsequently also reports `java` is not recognized. Per the task constraint, no Java/Maven toolchain was downloaded and no application was launched. Maven RED/GREEN execution is therefore unavailable in this environment.

2. **Offline static contract verification: PASS**

   A Python/PyYAML and XML parser check successfully parsed both manifests and both module POMs. It asserted:

   - OpenAPI 3.0.3 version, title/internal marking, `/` server, exact two paths, and exact operation verbs.
   - All five exact operation IDs for each service.
   - Exact success and error response-code sets, `Location` header paths, required Service A 502 responses, and absence of Service B 502 responses.
   - Exact DTO/API-error properties, required fields, data formats, and all required validation constraints.
   - Test-scoped, version-managed SnakeYAML dependencies in both POMs.
   - Exact preservation of every original application YAML key after removal of the newly appended `springdoc` block; each Swagger UI URL and disabled api-docs setting.
   - Controller annotation/path alignment, no product controller/client/service changes, and no `.ps1` or `.cmd` files.

   Output:

   ```text
   Static OpenAPI/YAML/XML/Java alignment checks: PASS
   ```

3. **Diff hygiene**

   ```text
   git diff --check
   ```

   Result: clean; Git emitted only non-failing CRLF conversion warnings for pre-existing-text file normalization rules.

## Self-review

- Removed an initially inaccurate claim that the controllers return an absolute `Location` URL; the manifests now correctly call it the created product path.
- Confirmed the Service B not-found example exactly matches `ProductNotFoundException` (`Product 1 was not found`).
- No controller, client, service, persistence, datasource, Flyway, JPA, or Feign behavior changed.

## Remaining limitation

The Java runtime and Maven executable are absent, so the checked-in Java contract tests could not be compiled or run here. The static validation above is the available verification evidence; run the recorded Maven command in a Java 21/Maven environment for runtime test execution.
