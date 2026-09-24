package unit;

import com.hungvers.idempotency.api.service.port.IdempotencyHasher;
import com.hungvers.idempotency.starter.hasher.JacksonIdempotencyHasher;

public class JacksonIdempotencyHasherTest extends IdempotencyHasherContractTest {
    private final JacksonIdempotencyHasher hasher = new JacksonIdempotencyHasher();

    @Override
    protected IdempotencyHasher hasher() {
        return hasher;
    }
}
