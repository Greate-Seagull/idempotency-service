package com.hungvers.idempotency.api.exception;

public abstract class IdempotencyException extends RuntimeException {
    protected IdempotencyException(String message) {
        super(message);
    }

    protected IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
