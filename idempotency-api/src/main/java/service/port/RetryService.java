package service.port;

import exception.RetryExecutionException;

import java.util.function.Supplier;

public interface RetryService {
    void execute(Class<? extends Exception> errorType, Runnable runnable) throws RetryExecutionException;
    <T> T execute(Class<? extends Exception> errorType, Supplier<T> runnable) throws RetryExecutionException;
}
