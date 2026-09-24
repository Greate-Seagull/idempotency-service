package com.hungvers.idempotency.api.service.port;

import com.hungvers.idempotency.api.model.helper.Claimable;
import com.hungvers.idempotency.api.model.schema.IdempotencyRecord;

import java.util.Optional;
import java.util.function.Supplier;

public interface IdempotencyStore {
    void save(String idempotencyKey, IdempotencyRecord recorded);

    void delete(String idempotencyKey);

    Claimable<IdempotencyRecord> tryClaim(String idempotencyKey, Supplier<IdempotencyRecord> creator);

    Optional<IdempotencyRecord> find(String idempotencyKey);
}
