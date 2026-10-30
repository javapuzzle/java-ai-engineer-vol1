package com.acme.retail.ch01.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translates exceptions from the service layer into HTTP responses.
 *
 * <p>{@code @RestControllerAdvice} applies to every {@code @RestController}
 * in this module. Keeping this mapping in one place means
 * {@link ProductService} can stay focused on business logic and never needs
 * to know what an HTTP status code is.
 */
@RestControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
