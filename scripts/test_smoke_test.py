"""Standard-library checks for smoke_test's argument validation and timeouts."""

import importlib.util
import io
import pathlib
import unittest
from unittest.mock import patch
from urllib import error


MODULE_PATH = pathlib.Path(__file__).with_name("smoke_test.py")
SPEC = importlib.util.spec_from_file_location("smoke_test", MODULE_PATH)
smoke_test = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(smoke_test)


class FakeResponse:
    def __init__(self, status, body):
        self.status = status
        self.body = body

    def __enter__(self):
        return self

    def __exit__(self, *unused):
        return False

    def getcode(self):
        return self.status

    def read(self, *unused):
        return self.body


class SmokeTestTimeoutTest(unittest.TestCase):

    def test_run_passes_the_configured_timeout_to_every_network_request(self):
        timeouts = []
        deleted = False

        def fake_urlopen(http_request, timeout):
            nonlocal deleted
            timeouts.append(timeout)
            method = http_request.get_method()
            url = http_request.full_url
            if method == "GET" and url.endswith("/api/products/1"):
                if deleted:
                    raise error.HTTPError(url, 404, "Not Found", {}, io.BytesIO(b"{}"))
                return FakeResponse(200, b'{"name":"Smoke Test Product"}')
            if method == "POST":
                return FakeResponse(201, b'{"id":1}')
            if method == "PUT":
                return FakeResponse(200, b'{"stockQuantity":7}')
            if method == "GET" and url.endswith("/api/products"):
                return FakeResponse(200, b'[{"id":1}]')
            if method == "DELETE":
                deleted = True
                return FakeResponse(204, b"")
            return FakeResponse(200, b"openapi: 3.0.3")

        with patch.object(smoke_test.request, "urlopen", side_effect=fake_urlopen):
            smoke_test.run("http://a", "http://b", timeout=2.75)

        self.assertEqual([2.75] * 8, timeouts)

    def test_rejects_non_positive_or_non_finite_timeouts_before_network_access(self):
        for value in ("0", "-1", "nan", "inf"):
            with self.assertRaises(SystemExit) as raised:
                smoke_test.parse_args(["--timeout", value])
            self.assertEqual(2, raised.exception.code)


if __name__ == "__main__":
    unittest.main()
