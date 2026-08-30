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
