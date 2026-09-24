package com.hungvers.idempotency.api.builder.rest;

public interface KeyStep {
    String idempotencyKey(String value);
}
