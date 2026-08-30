# Two Spring Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build two independently runnable Spring Boot services where Service A exposes Product CRUD and calls Service B through OpenFeign, while Service B persists Products in Flyway-managed SQLite and both services expose checked-in OpenAPI contracts through Swagger UI.

**Architecture:** A root Maven reactor builds `service-a` and `service-b`. Service A is a stateless public facade with an OpenFeign client; Service B owns validation, CRUD services, JPA, SQLite, and Flyway. DTOs are intentionally duplicated at the HTTP boundary so neither module has a compile-time dependency on the other.

**Tech Stack:** Java 21, Maven 3.9, Spring Boot 3.5.13, Spring Cloud 2025.0.2, Spring Cloud OpenFeign, Spring Data JPA, Hibernate community dialects, Xerial SQLite JDBC, Flyway, springdoc-openapi 2.8.9, JUnit 5, MockMvc, AssertJ, Mockito, SnakeYAML, Python 3 standard library.

**Spec:** `docs/superpowers/specs/2026-08-30-two-spring-services-design.md`

## Global Constraints

- Target Java 21 and package all source under `com.example.product`.
- Service A listens on `8080`; Service B listens on `8081`.
- Service A is the only public API and uses `/api/products`.
- Service B alone owns persistence and uses `/internal/products`.
- Service A calls every Service B operation through Spring Cloud OpenFeign.
- Service B uses SQLite locally; Flyway is the only schema authority and Hibernate uses `ddl-auto: validate`.
- Product fields and validation exactly match the approved specification.
- Both services serve checked-in OpenAPI 3.0 YAML and Swagger UI.
- HTTP errors use `timestamp`, `status`, `error`, `message`, `path`, and optional `fieldErrors`.
- Do not add `.ps1` or `.cmd` bootstrap scripts; portable helper tooling uses Python 3.
- Do not download a Java/Maven toolchain or launch the packaged services during this implementation session. Run build/tests only if a suitable existing toolchain is found.
- The current workspace is not a Git repository; initialize it once before the first commit and do not add build output, local databases, IDE metadata, or logs.

## File Map

### Root

- `pom.xml`: reactor, Java/compiler settings, Spring Boot parent, Spring Cloud and springdoc version management.
- `.gitignore`: Maven output, SQLite files, logs, and editor metadata.
- `README.md`: prerequisites, build/start workflow, Swagger URLs, API examples, and database reset.
- `scripts/smoke_test.py`: OS-agnostic CRUD and manifest smoke test against both running services.

### Service B

- `service-b/pom.xml`: web, validation, JPA, Flyway, SQLite, dialect, springdoc, and test dependencies.
- `service-b/src/main/java/com/example/product/serviceb/ServiceBApplication.java`: Spring Boot entry point.
- `service-b/src/main/java/com/example/product/serviceb/product/ProductEntity.java`: JPA-only persisted model.
- `service-b/src/main/java/com/example/product/serviceb/product/ProductRepository.java`: ordered repository access.
- `service-b/src/main/java/com/example/product/serviceb/product/ProductRequest.java`: validated create/update input.
- `service-b/src/main/java/com/example/product/serviceb/product/ProductResponse.java`: internal API response.
- `service-b/src/main/java/com/example/product/serviceb/product/ProductService.java`: CRUD and entity/DTO mapping.
- `service-b/src/main/java/com/example/product/serviceb/product/ProductController.java`: `/internal/products` HTTP contract.
- `service-b/src/main/java/com/example/product/serviceb/error/ProductNotFoundException.java`: typed 404 cause.
- `service-b/src/main/java/com/example/product/serviceb/error/ApiError.java`: standard error response.
- `service-b/src/main/java/com/example/product/serviceb/error/GlobalExceptionHandler.java`: validation and 404 mapping.
- `service-b/src/main/resources/application.yml`: ports, SQLite, Flyway, JPA, Swagger configuration.
- `service-b/src/main/resources/db/migration/V1__create_products.sql`: authoritative schema.
- `service-b/src/main/resources/static/openapi/service-b.yaml`: checked-in internal contract.
- `service-b/src/test/java/com/example/product/serviceb/product/ProductRepositoryTest.java`: SQLite/Flyway persistence proof.
- `service-b/src/test/java/com/example/product/serviceb/product/ProductControllerTest.java`: CRUD, validation, and 404 behavior.
- `service-b/src/test/java/com/example/product/serviceb/OpenApiContractTest.java`: YAML validity and required operations.
- `service-b/src/test/resources/application-test.yml`: isolated test database settings.

### Service A

- `service-a/pom.xml`: web, validation, OpenFeign, springdoc, and test dependencies.
- `service-a/src/main/java/com/example/product/servicea/ServiceAApplication.java`: Spring Boot entry point with Feign enabled.
- `service-a/src/main/java/com/example/product/servicea/product/ProductRequest.java`: validated public input.
- `service-a/src/main/java/com/example/product/servicea/product/ProductResponse.java`: public output.
- `service-a/src/main/java/com/example/product/servicea/product/ProductClient.java`: OpenFeign contract for Service B.
- `service-a/src/main/java/com/example/product/servicea/product/ProductController.java`: public facade.
- `service-a/src/main/java/com/example/product/servicea/error/ApiError.java`: public error response.
- `service-a/src/main/java/com/example/product/servicea/error/UpstreamServiceException.java`: typed 502 cause.
- `service-a/src/main/java/com/example/product/servicea/error/GlobalExceptionHandler.java`: validation, Feign 400/404, and 502 mapping.
- `service-a/src/main/resources/application.yml`: port, Service B URL/timeouts, and Swagger configuration.
- `service-a/src/main/resources/static/openapi/service-a.yaml`: checked-in public contract.
- `service-a/src/test/java/com/example/product/servicea/product/ProductControllerTest.java`: delegation and public HTTP semantics.
- `service-a/src/test/java/com/example/product/servicea/error/GlobalExceptionHandlerTest.java`: Feign/upstream mapping.
- `service-a/src/test/java/com/example/product/servicea/OpenApiContractTest.java`: YAML validity and required operations.

---

### Task 1: Maven Reactor and Bootable Service Skeletons

**Files:**
- Create: `.gitignore`
- Create: `pom.xml`
- Create: `service-a/pom.xml`
- Create: `service-b/pom.xml`
- Create: `service-a/src/main/java/com/example/product/servicea/ServiceAApplication.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/ServiceBApplication.java`
- Create: `service-a/src/test/java/com/example/product/servicea/ServiceAApplicationTest.java`
- Create: `service-b/src/test/java/com/example/product/serviceb/ServiceBApplicationTest.java`

**Interfaces:**
- Consumes: Java 21 and Maven 3.9 available on `PATH`.
- Produces: reactor modules `service-a` and `service-b`, plus Spring contexts named `ServiceAApplication` and `ServiceBApplication`.

- [ ] **Step 1: Initialize version control and ignore generated state**

Run `git init`, then create `.gitignore` with:

```gitignore
target/
**/target/
data/
**/*.db
**/*.db-shm
**/*.db-wal
*.log
.idea/
.vscode/
*.iml
.DS_Store
```

- [ ] **Step 2: Write context smoke tests before application classes**

```java
@SpringBootTest(properties = "spring.cloud.compatibility-verifier.enabled=false")
class ServiceAApplicationTest {
    @Test void contextLoads() {}
}
```

```java
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:target/context-test.db",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=false"
})
class ServiceBApplicationTest {
    @Test void contextLoads() {}
}
```

- [ ] **Step 3: Run tests and confirm the reactor is absent**

Run: `mvn -B test`

Expected: FAIL because the root `pom.xml` does not exist.

- [ ] **Step 4: Add the root and module POMs**

The root POM uses `spring-boot-starter-parent:3.5.13`, packaging `pom`, modules `service-a` and `service-b`, Java 21, `spring-cloud.version=2025.0.2`, and `springdoc.version=2.8.9`. Import `org.springframework.cloud:spring-cloud-dependencies` in `dependencyManagement`.

Service A dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-cloud-starter-openfeign`, `springdoc-openapi-starter-webmvc-ui:${springdoc.version}`, and `spring-boot-starter-test` for tests.

Service B dependencies: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`, `flyway-core`, `org.xerial:sqlite-jdbc`, `org.hibernate.orm:hibernate-community-dialects`, `springdoc-openapi-starter-webmvc-ui:${springdoc.version}`, and `spring-boot-starter-test` for tests.

Both module builds apply `spring-boot-maven-plugin`.

- [ ] **Step 5: Add minimal application entry points**

```java
package com.example.product.servicea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ServiceAApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceAApplication.class, args);
    }
}
```

```java
package com.example.product.serviceb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServiceBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
    }
}
```

- [ ] **Step 6: Run the skeleton tests**

Run: `mvn -B test`

Expected: PASS, two context tests executed.

- [ ] **Step 7: Commit**

```text
git add .gitignore pom.xml service-a service-b
git commit -m "build: scaffold two Spring Boot services"
```

### Task 2: Service B Flyway and SQLite Persistence

**Files:**
- Create: `service-b/src/main/java/com/example/product/serviceb/product/ProductEntity.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/product/ProductRepository.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/product/ProductRequest.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/product/ProductResponse.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/product/ProductService.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/error/ProductNotFoundException.java`
- Create: `service-b/src/main/resources/application.yml`
- Create: `service-b/src/main/resources/db/migration/V1__create_products.sql`
- Create: `service-b/src/test/resources/application-test.yml`
- Create: `service-b/src/test/java/com/example/product/serviceb/product/ProductRepositoryTest.java`
- Create: `service-b/src/test/java/com/example/product/serviceb/product/ProductServiceTest.java`

**Interfaces:**
- Consumes: Spring Data `JpaRepository<ProductEntity, Long>` and a Flyway-migrated `products` table.
- Produces: `ProductService.create`, `findAll`, `findById`, `update`, and `delete`, returning `ProductResponse`.

- [ ] **Step 1: Write the failing migration/repository test**

Use `@DataJpaTest`, `@ActiveProfiles("test")`, and `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`. Persist an entity named `Keyboard` with price `49.90` and stock `8`; flush, reload it, and assert the generated positive ID and all values. Query `JdbcTemplate` for `SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1` and assert it is `1`.

- [ ] **Step 2: Run the repository test and verify failure**

Run: `mvn -B -pl service-b -Dtest=ProductRepositoryTest test`

Expected: FAIL because Product persistence types and the migration do not exist.

- [ ] **Step 3: Add SQLite configuration and migration**

```yaml
server:
  port: 8081
spring:
  datasource:
    url: ${PRODUCT_DB_URL:jdbc:sqlite:./data/products.db}
    driver-class-name: org.sqlite.JDBC
  flyway:
    enabled: true
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

Test profile uses `jdbc:sqlite:target/products-test.db`. The SQL migration is:

```sql
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(120) NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 120),
    description VARCHAR(1000),
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity >= 0),
    created_at VARCHAR(35) NOT NULL,
    updated_at VARCHAR(35) NOT NULL
);
```

- [ ] **Step 4: Implement the entity and repository**

Map `ProductEntity` to `products`; use `GenerationType.IDENTITY`, `BigDecimal price`, integer stock, and `Instant` timestamps stored through JPA. `ProductRepository` extends `JpaRepository` and declares `List<ProductEntity> findAllByOrderByIdAsc()`.

- [ ] **Step 5: Run the repository test**

Run: `mvn -B -pl service-b -Dtest=ProductRepositoryTest test`

Expected: PASS and one successful Flyway migration row.

- [ ] **Step 6: Write failing ProductService CRUD tests**

With a mocked `ProductRepository`, assert:

```java
ProductResponse created = service.create(new ProductRequest("Keyboard", "Mechanical", new BigDecimal("49.90"), 8));
assertThat(created.name()).isEqualTo("Keyboard");
verify(repository).save(any(ProductEntity.class));
```

Also assert ascending list mapping, lookup/update of ID `42`, timestamp preservation on update, and `ProductNotFoundException` for missing get/update/delete.

- [ ] **Step 7: Implement DTOs and ProductService**

`ProductRequest` is a record with `@NotBlank`, `@Size(max=120)`, `@Size(max=1000)`, `@NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2)`, and `@NotNull @Min(0)`. `ProductResponse` is a record containing all seven fields. `ProductService` trims name, maps blank descriptions to `null`, assigns `Instant.now()` on create/update, preserves `createdAt`, and uses a private `requireProduct(long id)` method.

- [ ] **Step 8: Run Service B unit and persistence tests**

Run: `mvn -B -pl service-b test`

Expected: PASS.

- [ ] **Step 9: Commit**

```text
git add service-b
git commit -m "feat(service-b): add Flyway managed SQLite persistence"
```

### Task 3: Service B Internal CRUD API and Errors

**Files:**
- Create: `service-b/src/main/java/com/example/product/serviceb/product/ProductController.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/error/ApiError.java`
- Create: `service-b/src/main/java/com/example/product/serviceb/error/GlobalExceptionHandler.java`
- Create: `service-b/src/test/java/com/example/product/serviceb/product/ProductControllerTest.java`

**Interfaces:**
- Consumes: the Task 2 `ProductService` CRUD methods and Product DTOs.
- Produces: HTTP CRUD at `/internal/products` and the common JSON error schema.

- [ ] **Step 1: Write failing MockMvc controller tests**

Use `@WebMvcTest(ProductController.class)` and `@MockitoBean ProductService`. Cover:

- `POST /internal/products` with a valid JSON body returns 201, `Location: /internal/products/1`, and the response.
- `GET /internal/products` returns the mocked ordered list.
- `GET` and `PUT /internal/products/1` return the mocked product.
- `DELETE /internal/products/1` returns 204.
- A blank name, negative price, and negative stock return 400 with keys in `fieldErrors`.
- `ProductNotFoundException(99)` returns 404 and message `Product 99 was not found`.

- [ ] **Step 2: Verify the tests fail**

Run: `mvn -B -pl service-b -Dtest=ProductControllerTest test`

Expected: FAIL because the controller and handler do not exist.

- [ ] **Step 3: Implement ProductController**

Use `@RestController`, `@RequestMapping("/internal/products")`, constructor injection, `@Valid`, and these exact signatures:

```java
ResponseEntity<ProductResponse> create(ProductRequest request)
List<ProductResponse> findAll()
ProductResponse findById(long id)
ProductResponse update(long id, ProductRequest request)
ResponseEntity<Void> delete(long id)
```

Create returns `URI.create("/internal/products/" + response.id())`; delete calls the service before returning `noContent()`.

- [ ] **Step 4: Implement standard error mapping**

`ApiError` is a record containing `Instant timestamp`, `int status`, `String error`, `String message`, `String path`, and `Map<String,String> fieldErrors`. The handler maps `ProductNotFoundException` to 404 and `MethodArgumentNotValidException` to 400 using field name/default message pairs.

- [ ] **Step 5: Run Service B tests**

Run: `mvn -B -pl service-b test`

Expected: PASS.

- [ ] **Step 6: Commit**

```text
git add service-b
git commit -m "feat(service-b): expose internal Product CRUD API"
```

### Task 4: Service A OpenFeign Facade and Upstream Errors

**Files:**
- Create: `service-a/src/main/java/com/example/product/servicea/product/ProductRequest.java`
- Create: `service-a/src/main/java/com/example/product/servicea/product/ProductResponse.java`
- Create: `service-a/src/main/java/com/example/product/servicea/product/ProductClient.java`
- Create: `service-a/src/main/java/com/example/product/servicea/product/ProductController.java`
- Create: `service-a/src/main/java/com/example/product/servicea/error/ApiError.java`
- Create: `service-a/src/main/java/com/example/product/servicea/error/UpstreamServiceException.java`
- Create: `service-a/src/main/java/com/example/product/servicea/error/GlobalExceptionHandler.java`
- Create: `service-a/src/main/resources/application.yml`
- Create: `service-a/src/test/java/com/example/product/servicea/product/ProductControllerTest.java`
- Create: `service-a/src/test/java/com/example/product/servicea/error/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: Service B `/internal/products` JSON contract.
- Produces: public `/api/products` contract with Service B 400/404 preservation and other upstream failures mapped to 502.

- [ ] **Step 1: Write failing public controller tests**

Use `@WebMvcTest(ProductController.class)` and `@MockitoBean ProductClient`. Mirror Task 3's successful cases under `/api/products`, asserting that every method calls the corresponding `ProductClient` method. Assert public create rewrites `Location` to `/api/products/{id}`. Assert invalid public input is rejected before the client is called.

- [ ] **Step 2: Verify the controller tests fail**

Run: `mvn -B -pl service-a -Dtest=ProductControllerTest test`

Expected: FAIL because ProductClient, DTOs, and controller do not exist.

- [ ] **Step 3: Implement Service A DTOs, Feign client, and controller**

Duplicate the Task 2 validation annotations and response record. Define:

```java
@FeignClient(name = "service-b", url = "${service-b.base-url}")
interface ProductClient {
    @PostMapping("/internal/products") ProductResponse create(@RequestBody ProductRequest request);
    @GetMapping("/internal/products") List<ProductResponse> findAll();
    @GetMapping("/internal/products/{id}") ProductResponse findById(@PathVariable long id);
    @PutMapping("/internal/products/{id}") ProductResponse update(@PathVariable long id, @RequestBody ProductRequest request);
    @DeleteMapping("/internal/products/{id}") void delete(@PathVariable long id);
}
```

The public controller delegates one-for-one, supplies the public `Location` header, and returns 204 for delete.

- [ ] **Step 4: Add Service A runtime configuration**

```yaml
server:
  port: 8080
service-b:
  base-url: ${SERVICE_B_URL:http://localhost:8081}
spring:
  cloud:
    openfeign:
      client:
        config:
          service-b:
            connect-timeout: 2000
            read-timeout: 5000
```

- [ ] **Step 5: Write failing upstream error tests**

Construct `FeignException.NotFound`, `FeignException.BadRequest`, `FeignException.InternalServerError`, and `RetryableException` test instances. Through MockMvc, assert 404 and 400 retain a safe upstream message, while 500 and connection failure return 502. Assert every body has the common error fields and no stack trace.

- [ ] **Step 6: Implement Service A GlobalExceptionHandler**

Map local `MethodArgumentNotValidException` to 400. Map `FeignException` by status: 400 to 400, 404 to 404, every other status to 502. Map `RetryableException` and `UpstreamServiceException` to 502. Extract only the upstream `message` field from a JSON error body; use `Service B rejected the request`, `Product was not found`, or `Service B is unavailable` when parsing fails.

- [ ] **Step 7: Run Service A tests**

Run: `mvn -B -pl service-a test`

Expected: PASS.

- [ ] **Step 8: Commit**

```text
git add service-a
git commit -m "feat(service-a): add OpenFeign Product facade"
```

### Task 5: Checked-in OpenAPI Manifests and Swagger UI

**Files:**
- Create: `service-a/src/main/resources/static/openapi/service-a.yaml`
- Create: `service-b/src/main/resources/static/openapi/service-b.yaml`
- Modify: `service-a/src/main/resources/application.yml`
- Modify: `service-b/src/main/resources/application.yml`
- Create: `service-a/src/test/java/com/example/product/servicea/OpenApiContractTest.java`
- Create: `service-b/src/test/java/com/example/product/serviceb/OpenApiContractTest.java`

**Interfaces:**
- Consumes: public and internal CRUD mappings from Tasks 3 and 4.
- Produces: valid OpenAPI 3.0 documents and Swagger UI pages configured to load them.

- [ ] **Step 1: Write failing manifest contract tests**

Add SnakeYAML as a test dependency to both modules. Each test loads its YAML from the classpath into a `Map<String,Object>`, asserts `openapi` starts with `3.0`, and asserts the `paths` map contains its base collection and item paths. Assert collection operations are exactly `get` and `post`; item operations are exactly `get`, `put`, and `delete`. Assert `components.schemas` contains `ProductRequest`, `ProductResponse`, and `ApiError`.

- [ ] **Step 2: Verify manifest tests fail**

Run: `mvn -B -pl service-a,service-b -Dtest=OpenApiContractTest test`

Expected: FAIL because both YAML resources are absent.

- [ ] **Step 3: Write Service A's complete OpenAPI YAML**

Use `openapi: 3.0.3`, title `Product Service A API`, version `1.0.0`, server `/`, and tag `Products`. Document all five operations with operation IDs `createProduct`, `listProducts`, `getProduct`, `updateProduct`, and `deleteProduct`; include 201/200/204, 400, 404, and 502 responses as applicable. Define Product schemas with the approved constraints, ISO date-time timestamps, decimal and integer examples, and an ApiError schema with optional `fieldErrors`.

- [ ] **Step 4: Write Service B's complete OpenAPI YAML**

Use the same component constraints and examples, title `Product Service B Internal API`, internal paths, and no 502 responses. Keep operation IDs distinct by suffixing `Internal`.

- [ ] **Step 5: Point Swagger UI at the checked-in manifests**

Add to each `application.yml`:

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    url: /openapi/service-a.yaml
  api-docs:
    enabled: false
```

Service B uses `/openapi/service-b.yaml` instead.

- [ ] **Step 6: Run all contract and module tests**

Run: `mvn -B test`

Expected: PASS, including both manifest tests.

- [ ] **Step 7: Commit**

```text
git add service-a service-b
git commit -m "docs: add OpenAPI manifests and Swagger UI"
```

### Task 6: Local Documentation, Smoke Test, and End-to-End Verification

**Files:**
- Create: `README.md`
- Create: `scripts/smoke_test.py`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: packaged Service A and Service B JARs, ports 8080/8081, and all five public CRUD operations.
- Produces: reproducible local startup and a non-interactive end-to-end verification script.

- [ ] **Step 1: Write the smoke script assertions before running services**

The script uses only Python's standard library, calls `GET` on both YAML URLs, and performs create, retrieve, update, list, and delete against `http://localhost:8080/api/products`:

```python
created = request_json("POST", base_uri, {
    "name": "Smoke Test Product",
    "description": "Created through Service A",
    "price": 19.95,
    "stockQuantity": 5,
})
assert created["id"] > 0, "Create did not return an id."

loaded = request_json("GET", f'{base_uri}/{created["id"]}')
assert loaded["name"] == "Smoke Test Product", "Get returned the wrong product."

updated = request_json("PUT", f'{base_uri}/{created["id"]}', {
    "name": "Updated Smoke Product",
    "description": "Updated through Service A",
    "price": 24.50,
    "stockQuantity": 7,
})
assert updated["stockQuantity"] == 7, "Update did not persist."

all_products = request_json("GET", base_uri)
assert created["id"] in [item["id"] for item in all_products], "List omitted the created product."
request_json("DELETE", f'{base_uri}/{created["id"]}', expected_status=204)
assert_http_status(f'{base_uri}/{created["id"]}', 404)
```

Print `Smoke test passed: Service A -> OpenFeign -> Service B -> SQLite` only after all assertions succeed.

- [ ] **Step 2: Verify the Python utility without launching services**

Run: `python -m py_compile scripts/smoke_test.py`

Expected: PASS with no output.

Run: `python scripts/smoke_test.py --help`

Expected: exit 0 and display configurable `--service-a-url` and `--service-b-url` options.

- [ ] **Step 3: Write README with exact local workflow**

Document Java 21 and Maven 3.9 prerequisites; `mvn clean package`; starting Service B with `java -jar service-b/target/service-b-1.0.0-SNAPSHOT.jar`; starting Service A in another terminal; both Swagger and YAML URLs; `python scripts/smoke_test.py`; environment overrides `SERVICE_B_URL` and `PRODUCT_DB_URL`; curl and Python request examples; and removing `data/products.db`, `-shm`, and `-wal` only while Service B is stopped.

- [ ] **Step 4: Run automated verification only when an existing toolchain is available**

Search standard installation locations and environment variables for an existing Java 21 and Maven 3.9 installation. If found, run `mvn -B clean verify`. If absent, record build/test verification as unavailable without downloading anything; still perform static source, YAML, XML, and Python syntax checks.

- [ ] **Step 5: Perform static end-to-end contract verification**

Trace each public controller operation to the matching `ProductClient` operation and matching Service B controller operation. Parse both OpenAPI files and assert their paths/verbs correspond to those mappings. Confirm Service A has no database dependencies or datasource properties.

Expected: all five CRUD operations form a complete Service A -> OpenFeign -> Service B -> repository path.

- [ ] **Step 6: Verify persistence and Swagger configuration statically**

Confirm Service B alone declares JDBC/JPA/Flyway dependencies and datasource properties. Confirm its migration defines every entity column. Confirm both services configure `/swagger-ui.html` to load their checked-in YAML contract.

- [ ] **Step 7: Run portable artifact checks**

Run Python syntax compilation, XML parsing of all POM files, YAML parsing of both OpenAPI files, and repository searches for prohibited `.ps1` or `.cmd` bootstrap files.

- [ ] **Step 8: Record final verification evidence**

If an existing toolchain was found, record the output from `mvn -B clean verify`. Otherwise record the exact missing executables and the static checks that passed; do not claim runtime or test verification.

Expected: no downloaded toolchain and no launched service processes.

- [ ] **Step 9: Commit**

```text
git add .gitignore README.md scripts
git commit -m "docs: add local runbook and end-to-end smoke test"
```

## Completion Audit

Before reporting completion, inspect the current worktree and record evidence for every acceptance criterion:

- `mvn -B clean verify` proves the complete reactor and automated tests pass.
- `ProductClient` plus controller delegation tests prove Service A uses OpenFeign for all operations.
- Service A dependency/property searches and Service B SQLite integration tests prove persistence ownership.
- `flyway_schema_history` assertions and `V1__create_products.sql` prove Flyway migration execution.
- The controller/client/repository test chain proves public CRUD traverses both services into SQLite; the delivered Python smoke utility allows optional live verification by the user.
- SnakeYAML contract tests and static resource/configuration checks prove both checked-in manifests are valid and configured for serving.
- Springdoc configuration checks prove both Swagger pages are configured to load the checked-in manifests.
- Handler tests prove validation, not-found, and upstream 502 behavior.
- A clean-machine-oriented README review proves the workflow and configuration are documented.
