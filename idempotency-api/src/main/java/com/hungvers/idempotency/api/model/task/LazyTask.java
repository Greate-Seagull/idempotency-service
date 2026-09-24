package com.hungvers.idempotency.api.model.task;

import com.hungvers.idempotency.api.service.port.IdempotencySerializer;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LazyTask<I, O> {
    private final I request;
    private final Function<I, O> function;
    private final Class<O> outputType;
    private O response;
    private boolean computed;

    private LazyTask(I request, Class<O> outputType, Function<I, O> function) {
        this.request = request;
        this.function = function;
        this.outputType = outputType;
        this.response = null;
        this.computed = false;
    }

    public static <I, O> LazyTask<I, O> of(I request, Class<O> outputType, Function<I, O> function) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(outputType);
        Objects.requireNonNull(function);
        return new LazyTask<>(request, outputType, function);
    }

    public static <I> LazyTask<I, Void> noOutput(I request, Consumer<I> consumer) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(consumer);
        return new LazyTask<>(request, Void.class, i -> { consumer.accept(i); return null; });
    }

    public static <O> LazyTask<Void, O> noInput(Class<O> outputType, Supplier<O> supplier) {
        Objects.requireNonNull(outputType);
        Objects.requireNonNull(supplier);
        return new LazyTask<>(null, outputType, ignored -> supplier.get());
    }

    public static LazyTask<Void, Void> noInOut(Runnable runnable) {
        Objects.requireNonNull(runnable);
        return new LazyTask<>(null, Void.class, ignored -> { runnable.run(); return null; });
    }

    public I getRequest() {
        return request;
    }

    public O getResponse() {
        return response;
    }

    public void computeIfAbsent() {
        if (computed)
            return;

        response = function.apply(request);
        computed = true;
    }

    public void putIfAbsent(O response) {
        if (computed)
            return;
        if (outputType == Void.class)
            return;

        this.response = response;
        computed = true;
    }

    public void putIfAbsent(String serialized, IdempotencySerializer serializer) {
        putIfAbsent(serializer.deserialize(serialized, outputType));
    }
}
