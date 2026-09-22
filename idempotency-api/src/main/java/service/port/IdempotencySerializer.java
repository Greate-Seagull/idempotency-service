package service.port;

public interface IdempotencySerializer {
    String serialize(Object response);

    <O> O deserialize(String serialized, Class<O> outputType);
}
