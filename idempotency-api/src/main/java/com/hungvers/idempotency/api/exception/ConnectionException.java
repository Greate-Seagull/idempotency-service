package com.hungvers.idempotency.api.exception;

public class ConnectionException extends RuntimeException{
    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(Throwable e) {
        super(e);
    }
}
