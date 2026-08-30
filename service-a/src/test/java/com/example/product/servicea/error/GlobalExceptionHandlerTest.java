package com.example.product.servicea.error;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void copiesTheSafeMessageFromA400UpstreamResponse() {
        ApiError error = bodyOf(handler.handleFeignException(feignException(400, "{\"message\":\"Name must be unique\"}"), request()));

        assertSafeError(error, 400, "Bad Request", "Name must be unique");
    }

    @Test
    void copiesTheSafeMessageFromA404UpstreamResponse() {
        ApiError error = bodyOf(handler.handleFeignException(feignException(404, "{\"message\":\"Product 7 was not found\"}"), request()));

        assertSafeError(error, 404, "Not Found", "Product 7 was not found");
    }

    @Test
    void usesThe400FallbackWhenTheUpstreamBodyIsAbsentOrMalformed() {
        ApiError absent = bodyOf(handler.handleFeignException(feignException(400, null), request()));
        ApiError malformed = bodyOf(handler.handleFeignException(feignException(400, "not-json"), request()));

        assertSafeError(absent, 400, "Bad Request", "Service B rejected the request");
        assertSafeError(malformed, 400, "Bad Request", "Service B rejected the request");
    }

    @Test
    void usesThe404FallbackWhenTheUpstreamBodyIsAbsentOrMalformed() {
        ApiError absent = bodyOf(handler.handleFeignException(feignException(404, null), request()));
        ApiError malformed = bodyOf(handler.handleFeignException(feignException(404, "{\"message\":false}"), request()));

        assertSafeError(absent, 404, "Not Found", "Product was not found");
        assertSafeError(malformed, 404, "Not Found", "Product was not found");
    }

    @Test
    void mapsUnexpectedUpstreamStatusesToASafeBadGatewayResponse() {
        ApiError error = bodyOf(handler.handleFeignException(feignException(500, "{\"message\":\"SQL details\"}"), request()));

        assertSafeError(error, 502, "Bad Gateway", "Service B is unavailable");
    }

    @Test
    void mapsConnectionRetryFailuresToASafeBadGatewayResponse() {
        RetryableException exception = new RetryableException(
                -1,
                "Connection refused at http://service-b:8081",
                Request.HttpMethod.GET,
                new IOException("Connection refused"),
                null,
                upstreamRequest()
        );

        ApiError error = bodyOf(handler.handleRetryableException(exception, request()));

        assertSafeError(error, 502, "Bad Gateway", "Service B is unavailable");
    }

    @Test
    void mapsExplicitUpstreamUnavailableFailuresToASafeBadGatewayResponse() {
        ApiError error = bodyOf(handler.handleUpstreamServiceException(
                new UpstreamServiceException(new IllegalStateException("internal URL")), request()));

        assertSafeError(error, 502, "Bad Gateway", "Service B is unavailable");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/products/7");
        return request;
    }

    private FeignException feignException(int status, String body) {
        Response.Builder response = Response.builder()
                .status(status)
                .reason("upstream")
                .request(upstreamRequest())
                .headers(Map.of("Content-Type", List.of("application/json")));
        if (body != null) {
            response.body(body, StandardCharsets.UTF_8);
        }
        return FeignException.errorStatus("ProductClient#findById(long)", response.build());
    }

    private Request upstreamRequest() {
        return Request.create(
                Request.HttpMethod.GET,
                "http://service-b:8081/internal/products/7",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
    }

    private ApiError bodyOf(ResponseEntity<ApiError> response) {
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private void assertSafeError(ApiError error, int status, String reason, String message) {
        assertNotNull(error.timestamp());
        assertEquals(status, error.status());
        assertEquals(reason, error.error());
        assertEquals(message, error.message());
        assertEquals("/api/products/7", error.path());
        assertEquals(Map.of(), error.fieldErrors());
        assertFalse(error.message().contains("http://"));
        assertFalse(error.message().contains("Feign"));
        assertFalse(error.message().contains("Exception"));
    }
}
