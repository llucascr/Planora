package com.planora.backend.exception.handler;

import com.planora.backend.exception.DataAlreadyExistException;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.exception.ExceptionResponse;
import com.planora.backend.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@RestController
public class CustomEntityResponseHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAllException(Exception ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildResponse(ex.getMessage(), request));
    }

    @ExceptionHandler(DataNotFoundException.class)
    public final ResponseEntity<ExceptionResponse> handleDataNotFoundException(DataNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildResponse(ex.getMessage(), request));
    }

    @ExceptionHandler(DataAlreadyExistException.class)
    public final ResponseEntity<ExceptionResponse> handleDataAlreadyExistException(DataAlreadyExistException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildResponse(ex.getMessage(), request));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public final ResponseEntity<ExceptionResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildResponse(ex.getMessage(), request));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public final ResponseEntity<ExceptionResponse> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildResponse(ex.getMessage(), request));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public final ResponseEntity<ExceptionResponse> handleWebClientResponseException(WebClientResponseException ex, WebRequest request) {
        String message = "GitHub API error: " + ex.getStatusCode() + " - " + ex.getStatusText();
        return ResponseEntity.status(ex.getStatusCode()).body(buildResponse(message, request));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(buildResponse(message, request), HttpStatus.BAD_REQUEST);
    }

    private ExceptionResponse buildResponse(String message, WebRequest request) {
        return new ExceptionResponse(LocalDateTime.now(), message, request.getDescription(false));
    }
}
