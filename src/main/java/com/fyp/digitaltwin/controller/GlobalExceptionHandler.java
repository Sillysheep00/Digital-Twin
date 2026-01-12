package com.fyp.digitaltwin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 * 
 * Provides consistent error responses across the API.
 * 
 * Best Practice: Centralized exception handling makes the API
 * more maintainable and provides better error messages to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations.
     * 
     * Example: When @NotNull validation fails, this method
     * converts the exception into a user-friendly 400 Bad Request response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("message", "Invalid input parameters");
        response.put("details", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles cases where request body is missing or malformed.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMissingBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid request body");
        response.put("message", "Request body is missing or malformed");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}