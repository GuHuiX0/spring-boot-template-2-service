# Two Spring Boot Product Services

This repository demonstrates a small product API split into two Spring Boot
services. Service A is the public facade on port `8080`. It forwards every
product operation through OpenFeign to Service B, which listens on port `8081`.
Service B owns persistence: Flyway creates and validates the SQLite schema
before JPA uses `data/products.db`.

```text
Client -> Service A :8080 -> OpenFeign -> Service B :8081 -> Flyway-managed SQLite
```

## Prerequisites

- Java 21
- Maven 3.9 or newer
- Python 3 only when using the optional smoke client

## Build and test

From the repository root, run:

```sh
mvn -B clean verify
mvn -B clean package
```

## Start locally

Build the project first, then use two separate terminals. Start Service B
before Service A so that the public facade has an available OpenFeign target.

Terminal 1 (Service B, port `8081`):

```sh
java -jar service-b/target/service-b-1.0.0-SNAPSHOT.jar
```

Terminal 2 (Service A, port `8080`):

```sh
java -jar service-a/target/service-a-1.0.0-SNAPSHOT.jar
```

Service A is the public API. Service B exposes its `/internal/products` API
for the OpenFeign client and should not be treated as the client-facing entry
point.

## Configuration

Service B owns the SQLite database. Override its location before starting
Service B with `PRODUCT_DB_URL`; its default is
`jdbc:sqlite:./data/products.db`. The tracked `data/` directory supports this
documented repository-root launch. If you start Service B from another working
directory, create that directory's database parent first or override
`PRODUCT_DB_URL`.

Service A targets Service B through `SERVICE_B_URL`; its default is
`http://localhost:8081`. Set it before starting Service A when Service B uses
a different host or port.

## API documentation

The packaged OpenAPI manifests and Swagger UIs are available at:

- [Service A Swagger UI](http://localhost:8080/swagger-ui.html)
- [Service A OpenAPI manifest](http://localhost:8080/openapi/service-a.yaml)
- [Service B Swagger UI](http://localhost:8081/swagger-ui.html)
- [Service B OpenAPI manifest](http://localhost:8081/openapi/service-b.yaml)

Use the Service A Swagger UI as the primary browser manifest page for
exercising the complete public flow: Service A -> OpenFeign -> Service B ->
SQLite.

## Create a product

With both services running, send a public request to Service A. This is a
single-line `curl` command that uses only standard curl options:

```sh
curl -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" --data "{\"name\":\"Desk lamp\",\"description\":\"Dimmable LED lamp\",\"price\":29.95,\"stockQuantity\":12}"
```

For a standard-library Python alternative that performs create, retrieve,
update, list, delete, and deleted-resource checks, use the smoke client below.

## Smoke client

`scripts/smoke_test.py` only tests services that are already running. It never
starts or stops Java processes and never resets the database.

```sh
python scripts/smoke_test.py --timeout 10.0
```

Use alternate service locations when needed:

```sh
python scripts/smoke_test.py --service-a-url http://localhost:9080 --service-b-url http://localhost:9081 --timeout 15.0
```

The client first verifies both packaged OpenAPI manifests, then calls Service
A for the entire CRUD flow. It prints its success message only after deletion
and the final expected `404` check succeed. `--timeout` is a positive finite
per-request timeout in seconds and defaults to `10.0`.

## Reset the local database

Stop Service B first. Then manually delete only these files if you want a
fresh local database:

- `data/products.db`
- `data/products.db-shm`
- `data/products.db-wal`

On the next Service B start, Flyway recreates the schema. There is no automated
reset script so that deleting the database remains an intentional local action.

## Verification note

This implementation session performed static checks only: Java was not
installed, and it did not download a toolchain or launch either application.
Run `mvn -B clean verify` with Java 21 to compile and execute the Java test
suite, then start both services to verify Swagger and the live smoke client.
