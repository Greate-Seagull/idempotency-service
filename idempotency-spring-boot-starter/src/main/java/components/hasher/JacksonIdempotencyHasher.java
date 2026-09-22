package components.hasher;

import components.exception.ConnectionException;
import org.springframework.stereotype.Component;
import service.port.IdempotencyHasher;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class JacksonIdempotencyHasher implements IdempotencyHasher {
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .addModule(new SimpleModule()
                    .addSerializer(BigDecimal.class, new StripTrailingZeroSerializer())
                    .addSerializer(String.class, new TrimSerializer())
            )
            .build();

    public String hash(Object payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(MAPPER.writeValueAsBytes(payload));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ConnectionException(e);
        }
    }
}
