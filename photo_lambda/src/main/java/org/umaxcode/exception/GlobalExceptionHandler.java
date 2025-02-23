package org.umaxcode.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponse maxPictureSizeExceptionHandle(MaxUploadSizeExceededException ex, HttpServletRequest request) {

        return ErrorResponse.builder()
                .path(request.getRequestURI())
                .message("Picture size exceeds the allowed limit! - 5M")
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
