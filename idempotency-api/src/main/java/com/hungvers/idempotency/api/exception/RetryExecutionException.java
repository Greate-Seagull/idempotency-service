package com.hungvers.idempotency.api.exception;

public class RetryExecutionException extends RuntimeException {
    public RetryExecutionException(Throwable e) {
        super("Retry failed", e);
    }
}
