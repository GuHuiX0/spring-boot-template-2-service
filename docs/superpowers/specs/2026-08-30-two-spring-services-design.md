# Two Spring Services Design

## Goal

Build a locally runnable Spring Boot example containing two independent services. Service A exposes the public Product API and delegates to Service B through Spring Cloud OpenFeign. Service B owns a SQLite database whose schema is managed by Flyway. Checked-in OpenAPI files and Swagger UI make both APIs easy to inspect and call in a browser.

## Scope

The project provides complete CRUD operations for a Product resource, automated tests, local startup documentation, and an end-to-end smoke test. Authentication, service discovery, messaging, caching, container orchestration, and production database deployment are outside this initial template.

## Repository Structure

The repository is a Maven multi-module build:

```text
.
|-- pom.xml
|-- service-a/
|   |-- pom.xml
|   `-- src/
|-- service-b/
|   |-- pom.xml
|   `-- src/
|-- scripts/
|   `-- smoke_test.py
`-- README.md
```

Each module produces an independently runnable Spring Boot executable JAR. Java 21 is the target runtime. Dependency versions are managed centrally by the root Maven build using compatible Spring Boot and Spring Cloud BOMs.

## Architecture and Data Flow

Service A listens on port `8080` and is the public entry point. Its controller accepts and validates requests under `/api/products`, then calls a typed OpenFeign client. The client addresses Service B through the configurable property `service-b.base-url`, which defaults to `http://localhost:8081`.

Service B listens on port `8081`. Its internal controller receives calls under `/internal/products`, delegates business operations to a service layer, and persists products through Spring Data JPA. SQLite is the only local database for this design. The default database file is stored beneath a local `data/` directory and can be overridden through configuration.

The request path is:

```text
Swagger UI or API client
  -> Service A public controller
  -> OpenFeign client
  -> Service B internal controller
  -> Product service and repository
  -> SQLite
```

Service A does not connect to the database and Service B does not expose the public `/api` route. The service boundary is represented with explicit request and response DTOs rather than exposing JPA entities.

## Product Model

A product contains:

| Field | Type | Rules |
| --- | --- | --- |
| `id` | Long | Database-generated, positive, immutable |
| `name` | String | Required, trimmed, 1-120 characters |
| `description` | String | Optional, at most 1000 characters |
| `price` | Decimal | Required, zero or greater, two fractional digits |
| `stockQuantity` | Integer | Required, zero or greater |
| `createdAt` | Instant | Generated on creation, immutable |
| `updatedAt` | Instant | Generated on creation and changed on update |

Create and update requests contain `name`, `description`, `price`, and `stockQuantity`. Responses contain every field. A full update uses `PUT`; partial update semantics are not included.

## API Contract

Both services support the same CRUD semantics at different base paths:

| Operation | Service A | Service B | Success |
| --- | --- | --- | --- |
| Create | `POST /api/products` | `POST /internal/products` | `201 Created` with product and `Location` header |
| List | `GET /api/products` | `GET /internal/products` | `200 OK` with JSON array |
| Get | `GET /api/products/{id}` | `GET /internal/products/{id}` | `200 OK` with product |
| Update | `PUT /api/products/{id}` | `PUT /internal/products/{id}` | `200 OK` with updated product |
| Delete | `DELETE /api/products/{id}` | `DELETE /internal/products/{id}` | `204 No Content` |

The initial list endpoint returns all rows ordered by ascending ID. Pagination, filtering, and sorting parameters are outside this template.

## Persistence and Flyway

Service B uses the SQLite JDBC driver and Hibernate's community SQLite dialect. Hibernate schema generation is set to validation only; it must never create or mutate tables. Flyway is the schema authority and runs before JPA initialization.

Migration `V1__create_products.sql` creates the `products` table, including validation-friendly constraints and timestamp columns. SQLite's generated integer primary key supplies Product IDs. Test execution uses an isolated temporary SQLite file so tests exercise the same database engine and migration path as local runtime.

## OpenFeign and Failure Handling

Service A declares one OpenFeign interface matching Service B's internal API. The base URL is externalized and no service registry is required.

Normal HTTP errors from Service B are translated consistently:

- A Service B `404` becomes a Service A `404` response.
- A Service B validation `400` becomes a Service A `400` response.
- Connection failures, timeouts, malformed upstream responses, and unexpected Service B 5xx responses become `502 Bad Gateway` from Service A.

Both services return a common JSON error shape containing `timestamp`, `status`, `error`, `message`, and `path`. Validation failures additionally contain a `fieldErrors` object keyed by request field. Internal exception details and stack traces are not returned to callers.

## OpenAPI and Swagger UI

Each service contains a checked-in OpenAPI 3.0 YAML contract beneath `src/main/resources/static/openapi/`. Springdoc serves Swagger UI and is configured to load that service's YAML file:

- Service A UI: `http://localhost:8080/swagger-ui.html`
- Service A contract: `http://localhost:8080/openapi/service-a.yaml`
- Service B UI: `http://localhost:8081/swagger-ui.html`
- Service B contract: `http://localhost:8081/openapi/service-b.yaml`

The Service A contract is the primary browser manifest for exercising the complete flow. Both YAML documents describe all CRUD operations, schemas, validation rules, example payloads, and expected error responses. Tests parse the checked-in YAML and verify its documented paths align with controller request mappings.

## Configuration and Local Operation

Default configuration favors immediate local execution:

- Service A port: `8080`
- Service B port: `8081`
- Service B URL used by Service A: `http://localhost:8081`
- SQLite file: `./data/products.db`

Environment variables can override the Service B URL and database location. The README documents prerequisites, root build commands, how to start Service B before Service A, Swagger URLs, curl examples, and how to remove the local database for a clean start.

## Testing and Verification

Service B tests cover Flyway migration success, repository persistence, Product CRUD behavior, validation, and not-found handling against temporary SQLite databases.

Service A tests cover controller validation, response status mapping, successful delegation to the Feign abstraction, and translation of Feign/upstream failures. Contract tests parse both OpenAPI files and assert the required paths and operations exist.

Verification consists of:

1. Running the complete Maven test suite from the repository root.
2. Packaging both executable JARs.
3. Providing the OS-agnostic `scripts/smoke_test.py` utility to create, retrieve, update, list, and delete a Product through Service A when the services are running.
4. Confirming through automated tests that Service A delegates every operation, Service B persists through Flyway-managed SQLite, and both OpenAPI contracts describe the implemented routes.

Launching the packaged applications is not required during implementation verification. No `.ps1` or `.cmd` bootstrap scripts are included.

## Acceptance Criteria

The implementation is complete when:

- The root Maven build and all tests pass on Java 21.
- Service A calls Service B through OpenFeign for every Product operation.
- Service B alone owns the SQLite connection and all persistence code.
- Flyway creates the Product schema from a versioned SQL migration.
- CRUD requests sent to Service A produce the specified HTTP behavior and persist through Service B.
- Both checked-in OpenAPI YAML files are valid and accessible through their running services.
- Swagger UI on Service A can execute the complete public API.
- Error responses and upstream failure translation match this specification.
- The README and Python smoke-test script allow another developer to reproduce the local workflow.
