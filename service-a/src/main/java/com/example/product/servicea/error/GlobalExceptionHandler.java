package com.example.product.servicea.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int MAX_UPSTREAM_MESSAGE_LENGTH = 1000;
    private static final String SERVICE_B_UNAVAILABLE = "Service B is unavailable";
    private static final String SERVICE_B_REJECTED_REQUEST = "Service B rejected the request";
    private static final String PRODUCT_NOT_FOUND = "Product was not found";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest()
                .body(apiError(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException exception, HttpServletRequest request) {
        if (exception.status() == HttpStatus.BAD_REQUEST.value()) {
            return ResponseEntity.badRequest().body(apiError(
                    HttpStatus.BAD_REQUEST,
                    safeUpstreamMessage(exception).orElse(SERVICE_B_REJECTED_REQUEST),
                    request.getRequestURI(),
                    Map.of()
            ));
        }
        if (exception.status() == HttpStatus.NOT_FOUND.value()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError(
                    HttpStatus.NOT_FOUND,
                    safeUpstreamMessage(exception).orElse(PRODUCT_NOT_FOUND),
                    request.getRequestURI(),
                    Map.of()
            ));
        }
        return unavailable(request);
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ApiError> handleRetryableException(RetryableException exception, HttpServletRequest request) {
        return unavailable(request);
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ApiError> handleUpstreamServiceException(
            UpstreamServiceException exception,
            HttpServletRequest request
    ) {
        return unavailable(request);
    }

    private ResponseEntity<ApiError> unavailable(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(apiError(HttpStatus.BAD_GATEWAY, SERVICE_B_UNAVAILABLE, request.getRequestURI(), Map.of()));
    }

    private Optional<String> safeUpstreamMessage(FeignException exception) {
        try {
            Optional<ByteBuffer> responseBody = exception.responseBody();
            if (responseBody.isEmpty()) {
                return Optional.empty();
            }

            ByteBuffer buffer = responseBody.get().asReadOnlyBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            JsonNode message = objectMapper.readTree(bytes).path("message");
            if (!message.isTextual()) {
                return Optional.empty();
            }

            String text = message.textValue();
            if (text == null || text.isBlank() || text.length() > MAX_UPSTREAM_MESSAGE_LENGTH) {
                return Optional.empty();
            }
            return Optional.of(text);
        } catch (RuntimeException | java.io.IOException ignored) {
            return Optional.empty();
        }
    }

    private ApiError apiError(HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
