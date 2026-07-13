package com.bharath.financetracker.common.exception;

/**
 * The request is well-formed JSON but semantically invalid (bad month format, wrong category
 * type for the endpoint, ...). Maps to 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
