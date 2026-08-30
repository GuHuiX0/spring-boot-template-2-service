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

## Fix round 1: complete documented Product constraints

Addressed the Important review finding without changing the deferred Java contract-test-depth observation.

- Added the OpenAPI-compatible `pattern: '\S'` constraint to the `name` property in both `ProductRequest` and `ProductResponse` schemas of both manifests. The single-quoted YAML scalar preserves one backslash, so the pattern requires at least one non-whitespace character.
- Added `maximum: 99999999999999999.99` to the `price` property in both request and response schemas of both manifests. Existing `minimum: 0` and `multipleOf: 0.01` remain unchanged.
- Retained every existing name and price example; the static check confirms each is non-whitespace, within the documented range, and price-compatible with 0.01 increments.
- Did not change `OpenApiContractTest`; the deferred Minor observation concerned expanding that test's depth and was explicitly out of scope for this round.

### Focused RED check

Before editing the manifests, the following focused Python check was run:

```powershell
@'
from pathlib import Path
import yaml
for path in ('service-a/src/main/resources/static/openapi/service-a.yaml', 'service-b/src/main/resources/static/openapi/service-b.yaml'):
    schemas = yaml.safe_load(Path(path).read_text())['components']['schemas']
    for schema_name in ('ProductRequest', 'ProductResponse'):
        name = schemas[schema_name]['properties']['name']
        price = schemas[schema_name]['properties']['price']
        assert name['pattern'] == r'\S'
        assert str(price['maximum']) == '99999999999999999.99'
print('Focused constraint check: PASS')
'@ | python -
```

Output (expected failure before the fields existed):

```text
Traceback (most recent call last):
  File "<stdin>", line 8, in <module>
KeyError: 'pattern'
```

### Exact final static command and output

```powershell
@'
import re
from decimal import Decimal
from pathlib import Path
import yaml

EXPECTED_MAXIMUM = '99999999999999999.99'
EXPECTED_PATTERN = r'\S'

def scalar(node, *keys):
    for key in keys:
        node = {entry[0].value: entry[1] for entry in node.value}[key]
    return node.value

for service, package in (('service-a', 'servicea'), ('service-b', 'serviceb')):
    manifest = Path(service) / 'src/main/resources/static/openapi' / f'{service}.yaml'
    source = manifest.read_text()
    document = yaml.safe_load(source)
    tree = yaml.compose(source)
    request_java = Path(service) / f'src/main/java/com/example/product/{package}/product/ProductRequest.java'
    java = request_java.read_text()
    assert '@NotBlank' in java and '@Digits(integer = 17, fraction = 2)' in java
    for schema_name in ('ProductRequest', 'ProductResponse'):
        name = document['components']['schemas'][schema_name]['properties']['name']
        price = document['components']['schemas'][schema_name]['properties']['price']
        assert name['minLength'] == 1 and name['maxLength'] == 120 and name['pattern'] == EXPECTED_PATTERN
        assert re.search(name['pattern'], name['example']) and re.search(name['pattern'], '   ') is None
        assert price['minimum'] == 0 and price['multipleOf'] == 0.01
        maximum = scalar(tree, 'components', 'schemas', schema_name, 'properties', 'price', 'maximum')
        assert maximum == EXPECTED_MAXIMUM and Decimal(maximum) == Decimal(EXPECTED_MAXIMUM)
        example = Decimal(str(price['example']))
        assert Decimal('0') <= example <= Decimal(maximum) and example % Decimal('0.01') == 0
    for path_item in document['paths'].values():
        for operation in (path_item.get('post'), path_item.get('put')):
            if operation:
                example = operation['requestBody']['content']['application/json']['example']
                assert re.search(EXPECTED_PATTERN, example['name'])
                assert Decimal('0') <= Decimal(str(example['price'])) <= Decimal(EXPECTED_MAXIMUM)
print('Round 1 OpenAPI constraint/schema/example/Java alignment: PASS')
'@ | python -
git diff --check
```

Output:

```text
Round 1 OpenAPI constraint/schema/example/Java alignment: PASS
warning: in the working copy of 'service-a/src/main/resources/static/openapi/service-a.yaml', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'service-b/src/main/resources/static/openapi/service-b.yaml', LF will be replaced by CRLF the next time Git touches it
```

`git diff --check` completed successfully; the displayed warnings are non-failing CRLF conversion notices.

### Maven limitation

Maven and Java remain unavailable in this environment (`mvn` and `java` are not recognized PowerShell commands). No toolchain was downloaded and no application was launched, so this fix round has static verification only.
