package com.cloudnative.webapp.exceptions;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final HttpHeaders headers;
    public GlobalExceptionHandler() {
        this.headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().mustRevalidate());
        headers.setPragma("no-cache");
        headers.add("X-Content-Type-Options", "nosniff");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleMethodNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupportedException() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).build();
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleEmailViolationException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).build();
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleBadRequestsException(Exception e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(e.getMessage());
    }
}