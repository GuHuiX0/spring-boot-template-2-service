package com.example.product.serviceb;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

    @Test
    void checkedInManifestMatchesTheInternalProductContract() {
        Map<String, Object> api = loadManifest("/openapi/service-b.yaml");

        assertTrue(String.valueOf(api.get("openapi")).startsWith("3.0"));

        Map<String, Object> paths = map(api, "paths");
        assertEquals(Set.of("/internal/products", "/internal/products/{id}"), paths.keySet());
        assertEquals(Set.of("get", "post"), map(paths, "/internal/products").keySet());
        assertEquals(Set.of("get", "put", "delete"), map(paths, "/internal/products/{id}").keySet());

        assertEquals("createProductInternal", operationId(paths, "/internal/products", "post"));
        assertEquals("listProductsInternal", operationId(paths, "/internal/products", "get"));
        assertEquals("getProductInternal", operationId(paths, "/internal/products/{id}", "get"));
        assertEquals("updateProductInternal", operationId(paths, "/internal/products/{id}", "put"));
        assertEquals("deleteProductInternal", operationId(paths, "/internal/products/{id}", "delete"));

        assertTrue(map(map(api, "components"), "schemas").keySet()
                .containsAll(Set.of("ProductRequest", "ProductResponse", "ApiError")));
        for (String path : paths.keySet()) {
            for (String verb : map(paths, path).keySet()) {
                assertFalse(responses(paths, path, verb).containsKey("502"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadManifest(String path) {
        try (InputStream input = OpenApiContractTest.class.getResourceAsStream(path)) {
            assertNotNull(input, "checked-in OpenAPI manifest must be on the classpath");
            return (Map<String, Object>) new Yaml().load(input);
        } catch (Exception exception) {
            throw new AssertionError("could not load checked-in OpenAPI manifest", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> source, String key) {
        return (Map<String, Object>) source.get(key);
    }

    private String operationId(Map<String, Object> paths, String path, String verb) {
        return String.valueOf(map(map(paths, path), verb).get("operationId"));
    }

    private Map<String, Object> responses(Map<String, Object> paths, String path, String verb) {
        return map(map(map(paths, path), verb), "responses");
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
