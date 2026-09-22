package unit;

import org.junit.jupiter.api.Test;
import service.port.IdempotencyHasher;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class IdempotencyHasherContractTest {
    protected abstract IdempotencyHasher hasher();

    @Test
    void trims_leading_trailing_whitespace_in_strings() {
        var trailing = Map.of("name", "  hưng  ");
        var noTrail = Map.of("name", "hưng");

        var trailingHashed = hasher().hash(trailing);
        var noTrailHashed = hasher().hash(noTrail);

        assertEquals(
                noTrailHashed,
                trailingHashed
        );
    }

    @Test
    void strips_trailing_zero_in_decimals() {
        var trailing = Map.of("amount", new BigDecimal("1.50"));
        var noTrail = Map.of("amount", new BigDecimal("1.5"));

        var trailingHashed = hasher().hash(trailing);
        var noTrailHashed = hasher().hash(noTrail);

        assertEquals(
                trailingHashed,
                noTrailHashed
        );
    }

    @Test
    void ignores_key_order() {
        var a = new LinkedHashMap<String, Object>();
        a.put("x", 1); a.put("y", 2);
        var b = new LinkedHashMap<String, Object>();
        b.put("y", 2); b.put("x", 1);

        var firstHashed = hasher().hash(a);
        var secondHashed = hasher().hash(b);

        assertEquals(firstHashed, secondHashed);
    }

    @Test
    void is_deterministic_across_calls() {
        var payload = Map.of("a", 1);

        var firstHashed = hasher().hash(payload);
        var secondHashed = hasher().hash(payload);

        assertEquals(firstHashed, secondHashed);
    }
}
