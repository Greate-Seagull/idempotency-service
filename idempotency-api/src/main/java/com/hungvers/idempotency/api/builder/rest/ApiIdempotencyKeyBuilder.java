package com.hungvers.idempotency.api.builder.rest;

public class ApiIdempotencyKeyBuilder implements
        TenantStep,
        MethodStep,
        PathStep,
        KeyStep
{
    private final StringBuilder keyBuilder = new StringBuilder();

    private void append(String name) {
        keyBuilder.append(name).append(":");
    }

    @Override
    @SuppressWarnings("java:S4144") // step-builder pattern: same body, different staged return type by design
    public MethodStep tenant(String name) {
        append(name);
        return this;
    }

    @Override
    @SuppressWarnings("java:S4144") // step-builder pattern: same body, different staged return type by design
    public PathStep method(String name) {
        append(name);
        return this;
    }

    @Override
    @SuppressWarnings("java:S4144") // step-builder pattern: same body, different staged return type by design
    public KeyStep path(String name) {
        append(name);
        return this;
    }

    @Override
    public String idempotencyKey(String value) {
        return keyBuilder.append(value).toString();
    }
}
