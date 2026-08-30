# Task 4 Report: Service A OpenFeign Product Facade

## Scope and contract binding

Implemented the Service A public product facade in the assigned worktree from base `cc18082`. Service A now exposes `/api/products` and delegates all CRUD operations one-for-one through a typed OpenFeign client to Service B's committed `/internal/products` contract. Service A duplicates the product request/response records intentionally and has no compile dependency on Service B.

## Test-first work

The following test files were created before their production counterparts:

- `service-a/src/test/java/com/example/product/servicea/product/ProductControllerTest.java`
- `service-a/src/test/java/com/example/product/servicea/error/GlobalExceptionHandlerTest.java`

`ProductControllerTest` covers public create/list/get/update/delete behavior, public create location rewriting, exact client delegation arguments, and local blank-name/negative-price/negative-stock validation without an upstream call.

`GlobalExceptionHandlerTest` constructs Feign exceptions through Feign's response factory/builder APIs and tests 400/404 safe message copying, missing/malformed message fallbacks, 500 handling, retry/connection handling, and explicit unavailable handling. Each asserted error verifies timestamp, status, reason, message, public path, empty `fieldErrors`, and that client-facing messages do not contain URL, Feign, or exception detail.

Focused RED commands were intentionally not executed because neither Java nor Maven is installed in this environment:

```text
mvn -pl service-a -Dtest=ProductControllerTest test
mvn -pl service-a -Dtest=GlobalExceptionHandlerTest test
```

The commands remain unavailable; no Java toolchain was downloaded and no application was launched. Consequently, runtime RED/GREEN test execution is unavailable and has not been claimed.

## Implementation

- Added validated Service A `ProductRequest` and `ProductResponse` records matching Service B's HTTP DTO shape and validation annotations exactly.
- Added the required `@FeignClient(name = "service-b", url = "${service-b.base-url}")` with the five specified internal routes.
- Added `ProductController` under `/api/products`, constructor-injecting only `ProductClient`; create emits `201` with `/api/products/{id}`, list/get/update return `200`, and delete returns `204`.
- Added immutable `ApiError` with copied, insertion-stable, unmodifiable `fieldErrors` semantics matching Service B.
- Added `UpstreamServiceException` as the explicit typed runtime cause for unavailable-upstream handling.
- Added `GlobalExceptionHandler`:
  - local validation returns `400 Bad Request`, `Validation failed`, and deterministic field errors;
  - upstream 400 and 404 preserve only a safely parsed top-level textual JSON `message` (maximum 1000 characters);
  - absent, blank, non-textual, malformed, or unreadable upstream messages use stable fallbacks;
  - all other Feign statuses, retryable failures, and typed unavailable failures return `502 Bad Gateway` / `Service B is unavailable` without exposing upstream data.
- Added the requested Service A server, Service B URL, and OpenFeign timeout configuration.

## Static verification evidence

Performed without a Java runtime or Maven:

1. `git diff --check` completed with no whitespace diagnostics before staging.
2. Service A request and response record bodies were normalized and compared against Service B: both matched apart from package name.
3. The client contains all five exact mappings: POST/list GET/id GET/PUT/DELETE under `/internal/products`.
4. The controller exposes only the public `/api/products` route and creates `/api/products/{id}`.
5. The handler source contains the required 400, 404, and 502 branches plus all required fallback strings and 1000-character limit.
6. `service-a` has no persistence/JPA/JDBC/Flyway/datasource/Hibernate/SQLite references, and no `.ps1` or `.cmd` helpers exist in Service A.
7. Diffing Service B against `cc18082` reported no changed Service B files.
8. The exact configuration values were found: port `8080`, default Service B URL, 2000 ms connection timeout, and 5000 ms read timeout.
9. The locally cached Feign 13.6.1 bytecode was inspected to verify the factory/builder and retry constructor signatures used by the error-handler tests.

## Self-review

Reviewed the staged surface against the task brief. No persistence was added to Service A; Service B was not modified; no apps, toolchains, scripts, or subagents were used. The remaining limitation is runtime verification: compilation and tests require a Java/Maven environment that is absent by task constraint.

## Fix round 1

Addressed review findings without changing production code:

- `GlobalExceptionHandlerTest` now passes `(Long) null` to the Feign 13.6.1 `RetryableException` constructor, removing the `Long`/`Date` overload ambiguity.
- `ProductControllerTest` now makes the mocked `ProductClient` throw realistic Feign 400, Feign 404, and retryable connection exceptions. The MockMvc assertions exercise `@RestControllerAdvice` resolution and verify HTTP status, timestamp, public path, safe message, empty `fieldErrors`, and absence of `trace`/`exception` fields.
- `GlobalExceptionHandlerTest` now verifies blank textual and 1001-character top-level upstream messages fall back safely for both the 400 and 404 branches.

Covering test files:

- `service-a/src/test/java/com/example/product/servicea/product/ProductControllerTest.java`
- `service-a/src/test/java/com/example/product/servicea/error/GlobalExceptionHandlerTest.java`

The planned focused Maven command remains unavailable because Java and Maven are absent:

```text
mvn -pl service-a -Dtest=ProductControllerTest,GlobalExceptionHandlerTest test
```

Exact static coverage command and output:

```text
git diff --check; rg -n '\(Long\) null' service-a/src/test/java/com/example/product/servicea/error/GlobalExceptionHandlerTest.java service-a/src/test/java/com/example/product/servicea/product/ProductControllerTest.java; rg -n '@WebMvcTest|rendersAnUpstream400|rendersAnUpstream404|rendersRetryable|thenThrow\(' service-a/src/test/java/com/example/product/servicea/product/ProductControllerTest.java; rg -n 'blankMessage|overlongMessage|repeat\(1001\)' service-a/src/test/java/com/example/product/servicea/error/GlobalExceptionHandlerTest.java; $productionChanges = git diff --name-only HEAD -- service-a/src/main; if ($productionChanges) { $productionChanges } else { 'PRODUCTION_CHANGES=none' }; $java = Get-Command java -ErrorAction SilentlyContinue; $mvn = Get-Command mvn -ErrorAction SilentlyContinue; if ($java) { "JAVA=$($java.Source)" } else { 'JAVA=absent' }; if ($mvn) { "MAVEN=$($mvn.Source)" } else { 'MAVEN=absent' }

service-a/src/test/java/com/example/product/servicea/error/GlobalExceptionHandlerTest.java:78:                (Long) null,
service-a/src/test/java/com/example/product/servicea/product/ProductControllerTest.java:234:                (Long) null,
34:@WebMvcTest(ProductController.class)
190:    void rendersAnUpstream400ThroughTheControllerAdvice() throws Exception {
191:        when(productClient.findById(7L)).thenThrow(feignException(400, "{\"message\":\"Name must be unique\"}"));
209:    void rendersAnUpstream404ThroughTheControllerAdvice() throws Exception {
210:        when(productClient.findById(7L)).thenThrow(feignException(404, "{\"message\":\"Product 7 was not found\"}"));
228:    void rendersRetryableUpstreamFailuresThroughTheControllerAdvice() throws Exception {
229:        when(productClient.findById(7L)).thenThrow(new RetryableException(
42:        ApiError blankMessage = bodyOf(handler.handleFeignException(feignException(400, "{\"message\":\"   \"}"), request()));
43:        ApiError overlongMessage = bodyOf(handler.handleFeignException(feignException(400, "{\"message\":\"%s\"}".formatted("x".repeat(1001))), request()));
47:        assertSafeError(blankMessage, 400, "Bad Request", "Service B rejected the request");
48:        assertSafeError(overlongMessage, 400, "Bad Request", "Service B rejected the request");
55:        ApiError blankMessage = bodyOf(handler.handleFeignException(feignException(404, "{\"message\":\"   \"}"), request()));
56:        ApiError overlongMessage = bodyOf(handler.handleFeignException(feignException(404, "{\"message\":\"%s\"}".formatted("x".repeat(1001))), request()));
60:        assertSafeError(blankMessage, 404, "Not Found", "Product was not found");
61:        assertSafeError(overlongMessage, 404, "Not Found", "Product was not found");
PRODUCTION_CHANGES=none
JAVA=absent
MAVEN=absent
```

Static Feign 13.6.1 bytecode inspection also confirmed the two conflicting six-argument retry constructors and that the added `Long` cast selects the intended overload:

```text
d(ILjava/lang/String;Lfeign/Request$HttpMethod;Ljava/lang/Throwable;Ljava/lang/Long;Lfeign/Request;)V
d(ILjava/lang/String;Lfeign/Request$HttpMethod;Ljava/lang/Throwable;Ljava/util/Date;Lfeign/Request;)V
```

Self-review: the tests assert externally observable HTTP behavior rather than mock calls, except for the existing delegation checks where client invocation is itself the facade contract. No production files changed in this round. The deferred `status == -1` test was not added.
