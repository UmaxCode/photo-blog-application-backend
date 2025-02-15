package org.umaxcode.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PhotoBlogException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse PhotoBlogExceptionHandler(PhotoBlogException ex, HttpServletRequest request) {

        return ErrorResponse.builder()
                .path(request.getRequestURI())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse exceptionHandler(Exception ex, HttpServletRequest request) {

        return ErrorResponse.builder()
                .path(request.getRequestURI())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
