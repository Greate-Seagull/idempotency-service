package com.hungvers.idempotency.api.service.port;

import com.hungvers.idempotency.api.exception.RetryExecutionException;

import java.util.function.Supplier;

public interface RetryService {
    void execute(Class<? extends Exception> errorType, Runnable runnable) throws RetryExecutionException;
    <T> T execute(Class<? extends Exception> errorType, Supplier<T> runnable) throws RetryExecutionException;
}
