package components.serializer;

import org.springframework.stereotype.Component;
import service.port.IdempotencySerializer;
import tools.jackson.databind.ObjectMapper;

@Component
public class JacksonIdempotencySerializer implements IdempotencySerializer {
    private final ObjectMapper objectMapper;

    public JacksonIdempotencySerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object response) {
        return objectMapper.writeValueAsString(response);
    }

    @Override
    public <O> O deserialize(String serialized, Class<O> outputType) {
        return objectMapper.readValue(serialized, outputType);
    }
}
