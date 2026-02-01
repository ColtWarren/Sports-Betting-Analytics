package com.coltwarren.sports_betting_analytics.exception;

import org.springframework.http.HttpStatus;

/**
 * Base API Exception
 *
 * Parent class for all custom API exceptions.
 * Includes HTTP status and error code for proper error responses.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ApiException(String message, HttpStatus status) {
        this(message, status, "API_ERROR");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
