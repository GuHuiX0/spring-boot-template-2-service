package com.example.product.servicea;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

    @Test
    void checkedInManifestMatchesThePublicProductContract() {
        Map<String, Object> api = loadManifest("/openapi/service-a.yaml");

        assertTrue(String.valueOf(api.get("openapi")).startsWith("3.0"));

        Map<String, Object> paths = map(api, "paths");
        assertEquals(Set.of("/api/products", "/api/products/{id}"), paths.keySet());
        assertEquals(Set.of("get", "post"), map(paths, "/api/products").keySet());
        assertEquals(Set.of("get", "put", "delete"), map(paths, "/api/products/{id}").keySet());

        assertEquals("createProduct", operationId(paths, "/api/products", "post"));
        assertEquals("listProducts", operationId(paths, "/api/products", "get"));
        assertEquals("getProduct", operationId(paths, "/api/products/{id}", "get"));
        assertEquals("updateProduct", operationId(paths, "/api/products/{id}", "put"));
        assertEquals("deleteProduct", operationId(paths, "/api/products/{id}", "delete"));

        assertTrue(map(map(api, "components"), "schemas").keySet()
                .containsAll(Set.of("ProductRequest", "ProductResponse", "ApiError")));
        assertTrue(responses(paths, "/api/products", "post").containsKey("502"));
        assertTrue(responses(paths, "/api/products", "get").containsKey("502"));
        assertTrue(responses(paths, "/api/products/{id}", "get").containsKey("502"));
        assertTrue(responses(paths, "/api/products/{id}", "put").containsKey("502"));
        assertTrue(responses(paths, "/api/products/{id}", "delete").containsKey("502"));
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
