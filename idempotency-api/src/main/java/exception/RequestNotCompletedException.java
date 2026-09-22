package exception;

// 409
public class RequestNotCompletedException extends IdempotencyException {
    public RequestNotCompletedException() {
        super("The request is in progress");
    }
}
