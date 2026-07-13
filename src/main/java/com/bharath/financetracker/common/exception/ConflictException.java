package com.bharath.financetracker.common.exception;

/**
 * The request collides with existing state (duplicate email, category still in use, ...).
 * Maps to 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
