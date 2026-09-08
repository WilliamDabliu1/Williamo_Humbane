package com.williamo.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.williamo.dto.ErrorResponse;
import com.williamo.exception.OpenMeteoIntegrationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Request failed";
        log.warn("API error status={} message={}", ex.getStatusCode().value(), message);
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(ex.getStatusCode().value(), message));
    }

    @ExceptionHandler(OpenMeteoIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleOpenMeteo(OpenMeteoIntegrationException ex) {
        log.warn("Open-Meteo integration error source={} remoteStatus={} message={}",
                ex.getSource(), ex.getRemoteStatus(), ex.getMessage());
        ErrorResponse body = new ErrorResponse(ex.getStatus().value(), ex.getMessage());
        body.setSource(ex.getSource());
        body.setRemoteStatus(ex.getRemoteStatus());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        String message;
        if ("latitude".equals(name) || "longitude".equals(name)) {
            message = name + " must be a valid number";
        } else if ("id".equals(name) || "page".equals(name) || "size".equals(name)) {
            message = name + " must be a valid number";
        } else if ("startDate".equals(name) || "endDate".equals(name)) {
            message = name + " must be in format YYYY-MM-DD";
        } else {
            message = "Invalid value for parameter " + name;
        }
        log.warn("Invalid parameter {}: {}", name, ex.getValue());
        return toResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return toResponse(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return toResponse(HttpStatus.NOT_FOUND, "Endpoint not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return toResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error while processing the request");
    }

    private ResponseEntity<ErrorResponse> toResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }
}
