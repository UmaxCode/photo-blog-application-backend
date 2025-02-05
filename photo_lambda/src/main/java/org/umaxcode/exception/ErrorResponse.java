package org.umaxcode.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {

    private String path;
    private String message;
    private LocalDateTime timestamp;
}
