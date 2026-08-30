# Task 2 Report: Service B Flyway and SQLite Persistence

## Implementation

- Added Service B SQLite/Flyway configuration. Normal configuration uses `jdbc:sqlite:./data/products.db`, port `8081`, Flyway enabled, the Hibernate community SQLite dialect, `ddl-auto: validate`, and `open-in-view: false`.
- Added an isolated test profile at `jdbc:sqlite:target/service-b-test.db` with the same Flyway and JPA validation settings.
- Added the required `V1__create_products.sql` migration. It is the schema authority for the `products` table and contains all required checks, numeric precision, and `VARCHAR(35)` timestamp columns.
- Added `ProductEntity`, its Spring Data repository, request/response records, `ProductService`, `ProductNotFoundException`, and an `InstantStringConverter`. The converter stores public `Instant` values explicitly as ISO-8601 text and reads them back as `Instant`.
- Implemented the requested CRUD service interface. It trims names, normalizes null/blank descriptions to null, returns DTOs only, preserves `createdAt` during updates, updates `updatedAt`, and resolves missing records through private `requireProduct(long id)`.
- Added JPA/Flyway and mocked-service tests. The repository test persists/reloads the requested Keyboard fixture and asserts one successful Flyway migration. Service tests cover create, ascending listing, find ID 42, update timestamp preservation/advancement, delete, and missing get/update/delete behaviour.

## Files Changed

- `.superpowers/sdd/2026-08-30-two-spring-services/task-2-report.md`
- `service-b/src/main/java/com/example/product/serviceb/error/ProductNotFoundException.java`
- `service-b/src/main/java/com/example/product/serviceb/product/InstantStringConverter.java`
- `service-b/src/main/java/com/example/product/serviceb/product/ProductEntity.java`
- `service-b/src/main/java/com/example/product/serviceb/product/ProductRepository.java`
- `service-b/src/main/java/com/example/product/serviceb/product/ProductRequest.java`
- `service-b/src/main/java/com/example/product/serviceb/product/ProductResponse.java`
- `service-b/src/main/java/com/example/product/serviceb/product/ProductService.java`
- `service-b/src/main/resources/application.yml`
- `service-b/src/main/resources/db/migration/V1__create_products.sql`
- `service-b/src/test/java/com/example/product/serviceb/product/ProductRepositoryTest.java`
- `service-b/src/test/java/com/example/product/serviceb/product/ProductServiceTest.java`
- `service-b/src/test/resources/application-test.yml`

## Static Checks and Results

All static checks below passed.

- XML parser: `service-b/pom.xml` parsed successfully.
- YAML parser: both Service B configuration files parsed successfully.
- SQLite parser: executed the migration against an in-memory SQLite database successfully.
- Schema/entity comparison: SQL and `@Column` declarations match exactly in this order: `id`, `name`, `description`, `price`, `stock_quantity`, `created_at`, `updated_at`; the entity uses `InstantStringConverter` for both timestamp columns.
- Interface/annotation inspection: confirmed the repository inheritance and ordered query, required request validation annotations, response timestamp fields, all five public service methods plus `requireProduct(long)`, and the exception message construction.
- Test-coverage inspection: confirmed the required JPA test/profile/Flyway assertions and all requested create/list/find/update/delete/error behaviours.
- `git diff --check`: no whitespace errors.
- Scope check: `git diff -- service-a` was empty; no Service A persistence dependency or configuration changed.

## TDD Evidence

`ProductRepositoryTest` and `ProductServiceTest` were authored before the corresponding production types. The intended focused RED commands were:

```text
mvn -pl service-b -Dtest=ProductRepositoryTest test
mvn -pl service-b -Dtest=ProductServiceTest test
```

They could not be executed: environment checks reported `JAVA_COMMAND=False`, `JAVAC_COMMAND=False`, and `MVN_COMMAND=False`. No Java/Maven toolchain was downloaded and no application was launched. Consequently, no RED or GREEN runtime claim is made; the static checks above are the available verification evidence.

## Self-Review

- Confirmed Flyway remains schema authority and both normal/test Hibernate modes use `validate`.
- Confirmed the SQLite text timestamp requirement is explicit rather than delegated to the dialect.
- Confirmed the entity is internal to Service B and service output is mapped only through `ProductResponse`.
- Confirmed trimming, blank-description normalization, timestamp semantics, ascending list access, and exact not-found wording match the brief.
- Confirmed the work is limited to Service B plus this task report, with no POM/version changes and no helper scripts.

## Concerns

Runtime compilation and execution remain unverified because Java, `javac`, and Maven are absent. A Java 21/Maven-enabled environment should run the two focused tests and the Service B module test suite before deployment.
