package org.umaxcode.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private String path;
    private String message;
    private String timestamp;
}
