package unit;

import components.hasher.JacksonIdempotencyHasher;
import service.port.IdempotencyHasher;

public class JacksonIdempotencyHasherTest extends IdempotencyHasherContractTest {
    private final JacksonIdempotencyHasher hasher = new JacksonIdempotencyHasher();

    @Override
    protected IdempotencyHasher hasher() {
        return hasher;
    }
}
