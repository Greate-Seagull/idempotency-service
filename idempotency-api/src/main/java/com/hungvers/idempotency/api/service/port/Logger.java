package com.hungvers.idempotency.api.service.port;

public interface Logger {
    void warn(String message, Throwable throwable);
}
