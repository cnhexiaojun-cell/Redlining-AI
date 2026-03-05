package com.redlining.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        String detail = e.getReason() != null ? e.getReason() : "Error";
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("detail", detail));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFound(NoResourceFoundException e) {
        String path = e.getResourcePath() != null ? e.getResourcePath() : "unknown";
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("detail", "Not found: " + path));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("detail", "Analysis failed: " + e.getMessage()));
    }
}
