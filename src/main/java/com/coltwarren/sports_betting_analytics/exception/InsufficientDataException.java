package com.coltwarren.sports_betting_analytics.exception;

import org.springframework.http.HttpStatus;

/**
 * Insufficient Data Exception
 *
 * Thrown when there's not enough data to perform an analysis
 * (e.g., missing xG data, no historical patterns).
 */
public class InsufficientDataException extends ApiException {

    public InsufficientDataException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_DATA");
    }

    public InsufficientDataException(String entity, String reason) {
        super(String.format("Insufficient data for %s: %s", entity, reason),
              HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_DATA");
    }
}
