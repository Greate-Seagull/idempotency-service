package components.store;

import exception.ConnectionException;
import model.helper.Claimable;
import model.schema.IdempotencyRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import service.port.IdempotencyStore;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class RedisIdempotencyStore implements IdempotencyStore {
    private final StringRedisTemplate template;
    private final ObjectMapper mapper;

    private static final String PREFIX = "idempotency:";

    public RedisIdempotencyStore(StringRedisTemplate template, ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    private static String formatKey(String idempotencyKey) {
        return PREFIX + idempotencyKey;
    }

    private static <T> T execute(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public void save(String idempotencyKey, IdempotencyRecord recorded) {
        execute(() -> {
            String value = mapper.writeValueAsString(recorded);
            template.opsForValue().set(formatKey(idempotencyKey), value, recorded.ttl());
            return null;
        });
    }

    @Override
    public void delete(String idempotencyKey) {
        execute(() -> {
            template.delete(formatKey(idempotencyKey));
            return null;
        });
    }

    public boolean saveIfAbsent(String idempotencyKey, IdempotencyRecord recorded) {
        Boolean result = execute(() -> {
            String value = mapper.writeValueAsString(recorded);
            return template.opsForValue().setIfAbsent(formatKey(idempotencyKey), value, recorded.ttl());
        });
        if (result == null)
            throw new ConnectionException("Uncertain claim result (null response) from Redis, key=" + idempotencyKey);
        return result;
    }

    public Optional<IdempotencyRecord> find(String idempotencyKey) {
        return execute(() -> {
            String value = template.opsForValue().get(formatKey(idempotencyKey));
            if (value == null)
                return Optional.empty();
            return Optional.of(mapper.readValue(value, IdempotencyRecord.class));
        });
    }

    @Override
    public Claimable<IdempotencyRecord> tryClaim(String idempotencyKey, Supplier<IdempotencyRecord> creator) {
        var success = saveIfAbsent(idempotencyKey, creator.get());
        var result = find(idempotencyKey).orElseThrow(() -> new ConnectionException("Idempotency key vanished between claim conflict and lookup, key=" + idempotencyKey));
        return Claimable.of(success, result);
    }
}
