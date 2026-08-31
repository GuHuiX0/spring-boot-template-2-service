package com.example.product.servicea;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiContractTest {

    @Test
    void checkedInManifestMatchesThePublicProductContract() {
        Map<String, Object> api = loadYaml("/openapi/service-a.yaml");
        assertEquals("3.0.3", api.get("openapi"));

        Map<String, Object> paths = map(api, "paths");
        assertEquals(Set.of("/api/products", "/api/products/{id}"), paths.keySet());
        assertEquals(Set.of("get", "post"), map(paths, "/api/products").keySet());
        assertEquals(Set.of("get", "put", "delete"), map(paths, "/api/products/{id}").keySet());
        assertEquals("listProducts", operationId(paths, "/api/products", "get"));
        assertEquals("createProduct", operationId(paths, "/api/products", "post"));
        assertEquals("getProduct", operationId(paths, "/api/products/{id}", "get"));
        assertEquals("updateProduct", operationId(paths, "/api/products/{id}", "put"));
        assertEquals("deleteProduct", operationId(paths, "/api/products/{id}", "delete"));
        assertResponses(paths, "/api/products", "get", Set.of("200", "502"));
        assertResponses(paths, "/api/products", "post", Set.of("201", "400", "502"));
        assertResponses(paths, "/api/products/{id}", "get", Set.of("200", "404", "502"));
        assertResponses(paths, "/api/products/{id}", "put", Set.of("200", "400", "404", "502"));
        assertResponses(paths, "/api/products/{id}", "delete", Set.of("204", "404", "502"));
        Map<String, Object> createHeaders = map(map(responses(paths, "/api/products", "post"), "201"), "headers");
        assertEquals("string", map(map(createHeaders, "Location"), "schema").get("type"));

        Map<String, Object> components = map(api, "components");
        Map<String, Object> schemas = map(components, "schemas");
        Map<String, Object> productResponse = map(schemas, "ProductResponse");
        assertEquals(Set.of("id", "name", "description", "price", "stockQuantity", "createdAt", "updatedAt"), Set.copyOf(list(productResponse, "required")));
        assertEquals(1, number(map(map(productResponse, "properties"), "id"), "minimum"));
        assertEquals(1, number(map(map(components, "parameters"), "ProductId"), "schema"), "minimum"));
    }

    @Test
    void applicationServesOnlyTheCheckedInPublicManifest() {
        Map<String, Object> config = loadYaml("/application.yml");
        Map<String, Object> springdoc = map(config, "springdoc");
        assertEquals("/swagger-ui.html", map(springdoc, "swagger-ui").get("path"));
        assertEquals("/openapi/service-a.yaml", map(springdoc, "swagger-ui").get("url"));
        assertEquals(false, map(springdoc, "api-docs").get("enabled"));
    }

    private void assertResponses(Map<String, Object> paths, String path, String verb, Set<String> expected) {
        assertEquals(expected, responses(paths, path, verb).keySet());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String path) {
        try (InputStream input = OpenApiContractTest.class.getResourceAsStream(path)) {
            assertNotNull(input, "checked-in YAML must be on the classpath");
            return (Map<String, Object>) new Yaml().load(input);
        } catch (Exception exception) {
            throw new AssertionError("could not load checked-in YAML", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<?, ?> source, String key) {
        return (Map<String, Object>) source.get(key);
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Map<String, Object> source, String key) {
        return (List<Object>) source.get(key);
    }

    private Number number(Map<String, Object> source, String key) {
        return (Number) source.get(key);
    }

    private Map<String, Object> responses(Map<String, Object> paths, String path, String verb) {
        return map(map(map(paths, path), verb), "responses");
    }

    private String operationId(Map<String, Object> paths, String path, String verb) {
        return String.valueOf(map(map(paths, path), verb).get("operationId"));
    }
}
