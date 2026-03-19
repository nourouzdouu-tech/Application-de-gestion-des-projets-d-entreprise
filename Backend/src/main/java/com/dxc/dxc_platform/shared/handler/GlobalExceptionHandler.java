package com.dxc.dxc_platform.shared.handler;

import com.dxc.dxc_platform.shared.dto.ApiErrorResponse;
import com.dxc.dxc_platform.shared.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404)
                .body(ApiErrorResponse.of(404, "NOT_FOUND", ex.getMessage(), req.getRequestURI(), ex.getCode()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return ResponseEntity.status(409)
                .body(ApiErrorResponse.of(409, "CONFLICT", ex.getMessage(), req.getRequestURI(), ex.getCode()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return ResponseEntity.status(401)
                .body(ApiErrorResponse.of(401, "UNAUTHORIZED", ex.getMessage(), req.getRequestURI(), ex.getCode()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return ResponseEntity.status(403)
                .body(ApiErrorResponse.of(403, "FORBIDDEN", ex.getMessage(), req.getRequestURI(), ex.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest req) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "VALIDATION_ERROR", message, req.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return ResponseEntity.internalServerError()
                .body(ApiErrorResponse.of(500, "INTERNAL_SERVER_ERROR", ex.getMessage(), req.getRequestURI(), null));
    }
}