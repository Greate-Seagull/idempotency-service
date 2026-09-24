package com.hungvers.idempotency.api.model.schema;

import java.time.Duration;

public record IdempotencyRecord(String request, String response, RecordStatus status, Duration ttl) {

    public static IdempotencyRecord inProgress(String request, Duration ttl) {
        return new IdempotencyRecord(
                request,
                null,
                RecordStatus.IN_PROGRESS,
                ttl
        );
    }

    public boolean isDuplicate(String hashed) {
        return request.equals(hashed);
    }

    public boolean isCompleted() {
        return status.equals(RecordStatus.COMPLETED);
    }

    public IdempotencyRecord completed(String response) {
        return new IdempotencyRecord(request, response, RecordStatus.COMPLETED, ttl);
    }
}
