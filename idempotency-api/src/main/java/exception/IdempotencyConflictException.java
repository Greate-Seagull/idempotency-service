package exception;

// 409
public class IdempotencyConflictException extends IdempotencyException {
    public IdempotencyConflictException() {
        super("The same idempotency key is used while request is different");
    }
}
