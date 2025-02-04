package org.umaxcode.exception;

import java.util.HashMap;
import java.util.Map;

public class ImageProcessingException extends RuntimeException {

    private final Map<String, String> errorDetails = new HashMap<>();

    public ImageProcessingException(Map<String, String> errorDetails) {
        super();
        this.errorDetails.putAll(errorDetails);
    }

    public Map<String, String> getErrorDetails() {
        return errorDetails;
    }

}
