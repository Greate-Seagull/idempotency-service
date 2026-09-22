package builder.rest;

public interface KeyStep {
    String idempotencyKey(String value);
}
