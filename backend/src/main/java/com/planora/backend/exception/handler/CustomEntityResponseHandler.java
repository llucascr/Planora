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
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@RestController
public class CustomEntityResponseHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class) // Global
    public final ResponseEntity<ExceptionResponse> handleAllException(Exception ex, WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataNotFoundException.class)
    public final ResponseEntity<ExceptionResponse> handleDataNotFoundException(DataNotFoundException ex, WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataAlreadyExistException.class)
    public final ResponseEntity<ExceptionResponse> handleDataNotFoundException(DataAlreadyExistException ex, WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public final ResponseEntity<ExceptionResponse> handleDataNotFoundException(BadCredentialsException ex, WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public final ResponseEntity<ExceptionResponse> handleDataNotFoundException(UnauthorizedException ex, WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
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
        ExceptionResponse response = new ExceptionResponse(
                LocalDateTime.now(),
                message,
                request.getDescription(false)
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}