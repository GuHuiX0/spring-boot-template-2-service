"""Exercise the public product API through Service A.

Both services must already be running. This script never starts, stops, or
resets a service or database.
"""

import argparse
import json
import math
import sys
from typing import Any, Optional, Sequence
from urllib import error, request


ERROR_BODY_LIMIT = 4096
PASS_MESSAGE = "Smoke test passed: Service A -> OpenFeign -> Service B -> SQLite"


class SmokeTestError(RuntimeError):
    """An expected smoke-test failure that should be reported without a traceback."""


def normalize_url(url: str) -> str:
    return url.rstrip("/")


def _read_bounded_body(response: Any) -> str:
    body = response.read(ERROR_BODY_LIMIT + 1)
    suffix = ""
    if len(body) > ERROR_BODY_LIMIT:
        body = body[:ERROR_BODY_LIMIT]
        suffix = "... (truncated)"
    return body.decode("utf-8", errors="replace").strip() + suffix


def _http_error_message(method: str, url: str, status: int, response: Any) -> str:
    body = _read_bounded_body(response)
    detail = body if body else "(empty response body)"
    return f"{method} {url} returned HTTP {status}: {detail}"


def _connection_error_message(url: str, exc: error.URLError) -> str:
    return (
        f"Could not contact {url}: {exc.reason}. "
        "Both Service A and Service B must already be running."
    )


def _parse_json(body: bytes, method: str, url: str, status: int) -> Any:
    if not body:
        return None
    try:
        return json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise SmokeTestError(
            f"{method} {url} returned HTTP {status} with an invalid JSON response"
        ) from exc


def request_json(
    method: str, url: str, body: Any = None, expected_status: int = 200, timeout: float = 10.0
) -> Any:
    """Send a JSON request and require exactly ``expected_status``."""
    data = json.dumps(body).encode("utf-8") if body is not None else None
    http_request = request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method=method,
    )
    try:
        with request.urlopen(http_request, timeout=timeout) as response:
            status = response.getcode()
            response_body = response.read()
    except error.HTTPError as exc:
        if exc.code == expected_status:
            return _parse_json(exc.read(), method, url, exc.code)
        raise SmokeTestError(_http_error_message(method, url, exc.code, exc)) from exc
    except error.URLError as exc:
        raise SmokeTestError(_connection_error_message(url, exc)) from exc

    if status != expected_status:
        raise SmokeTestError(
            f"{method} {url} returned HTTP {status}; expected HTTP {expected_status}"
        )
    return _parse_json(response_body, method, url, status)


def request_text(method: str, url: str, expected_status: int = 200, timeout: float = 10.0) -> str:
    """Send a text request and require exactly ``expected_status``."""
    http_request = request.Request(url, method=method)
    try:
        with request.urlopen(http_request, timeout=timeout) as response:
            status = response.getcode()
            response_body = response.read()
    except error.HTTPError as exc:
        raise SmokeTestError(_http_error_message(method, url, exc.code, exc)) from exc
    except error.URLError as exc:
        raise SmokeTestError(_connection_error_message(url, exc)) from exc

    if status != expected_status:
        raise SmokeTestError(
            f"{method} {url} returned HTTP {status}; expected HTTP {expected_status}"
        )
    return response_body.decode("utf-8", errors="replace")


def expect_not_found(url: str, timeout: float) -> Any:
    """Treat only a 404 response for a deleted product as success."""
    return request_json("GET", url, expected_status=404, timeout=timeout)


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise SmokeTestError(message)


def _require_product_id(product: Any) -> int:
    _require(isinstance(product, dict), "Create response was not a JSON object")
    product_id = product.get("id")
    _require(
        isinstance(product_id, int) and not isinstance(product_id, bool) and product_id > 0,
        "Create response did not contain a positive numeric ID",
    )
    return product_id


def _attempt_cleanup(service_a_url: str, product_id: int, timeout: float) -> None:
    try:
        request_json("DELETE", f"{service_a_url}/api/products/{product_id}", expected_status=204, timeout=timeout)
    except Exception as cleanup_error:  # Preserve the original assertion or request failure.
        print(f"Cleanup deletion for product {product_id} failed: {cleanup_error}", file=sys.stderr)


def run(service_a_url: str, service_b_url: str, timeout: float = 10.0) -> None:
    """Run the manifest checks and public CRUD flow, cleaning up on later failure."""
    service_a_url = normalize_url(service_a_url)
    service_b_url = normalize_url(service_b_url)

    manifest_a = request_text("GET", f"{service_a_url}/openapi/service-a.yaml", timeout=timeout)
    _require(manifest_a.strip(), "Service A OpenAPI manifest was empty")
    manifest_b = request_text("GET", f"{service_b_url}/openapi/service-b.yaml", timeout=timeout)
    _require(manifest_b.strip(), "Service B OpenAPI manifest was empty")

    product_id = None
    deleted = False
    try:
        created = request_json(
            "POST",
            f"{service_a_url}/api/products",
            {
                "name": "Smoke Test Product",
                "description": "Created through Service A",
                "price": 19.95,
                "stockQuantity": 5,
            },
            expected_status=201,
            timeout=timeout,
        )
        product_id = _require_product_id(created)

        retrieved = request_json("GET", f"{service_a_url}/api/products/{product_id}", timeout=timeout)
        _require(
            isinstance(retrieved, dict) and retrieved.get("name") == "Smoke Test Product",
            "Retrieved product did not have the expected name",
        )

        updated = request_json(
            "PUT",
            f"{service_a_url}/api/products/{product_id}",
            {
                "name": "Updated Smoke Product",
                "description": "Updated through Service A",
                "price": 24.50,
                "stockQuantity": 7,
            },
            timeout=timeout,
        )
        _require(
            isinstance(updated, dict) and updated.get("stockQuantity") == 7,
            "Updated product did not have stockQuantity 7",
        )

        products = request_json("GET", f"{service_a_url}/api/products", timeout=timeout)
        _require(
            isinstance(products, list)
            and any(isinstance(product, dict) and product.get("id") == product_id for product in products),
            "Created product ID was not present in the product list",
        )

        request_json(
            "DELETE",
            f"{service_a_url}/api/products/{product_id}",
            expected_status=204,
            timeout=timeout,
        )
        deleted = True
        expect_not_found(f"{service_a_url}/api/products/{product_id}", timeout)
    except Exception:
        if product_id is not None and not deleted:
            _attempt_cleanup(service_a_url, product_id, timeout)
        raise


def positive_finite_timeout(value: str) -> float:
    try:
        timeout = float(value)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("timeout must be a positive finite number") from exc
    if not math.isfinite(timeout) or timeout <= 0:
        raise argparse.ArgumentTypeError("timeout must be a positive finite number")
    return timeout


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Exercise an already-running Service A -> Service B product flow."
    )
    parser.add_argument("--service-a-url", default="http://localhost:8080")
    parser.add_argument("--service-b-url", default="http://localhost:8081")
    parser.add_argument(
        "--timeout",
        type=positive_finite_timeout,
        default=10.0,
        metavar="SECONDS",
        help="positive finite per-request timeout in seconds (default: 10.0)",
    )
    return parser.parse_args(argv)


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)
    try:
        run(args.service_a_url, args.service_b_url, args.timeout)
    except SmokeTestError as exc:
        print(f"Smoke test failed: {exc}", file=sys.stderr)
        return 1
    print(PASS_MESSAGE)
    return 0


if __name__ == "__main__":
    sys.exit(main())
