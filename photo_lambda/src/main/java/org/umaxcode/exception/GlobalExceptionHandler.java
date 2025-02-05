package org.umaxcode.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PhotoBlogException.class)
    public ErrorResponse PhotoBlogExceptionHandler(PhotoBlogException ex, HttpServletRequest request) {

        return ErrorResponse.builder()
                .path(request.getRequestURI())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
