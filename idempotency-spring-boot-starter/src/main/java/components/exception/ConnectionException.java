package components.exception;

public class ConnectionException extends RuntimeException{
    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(Throwable e) {
        super(e);
    }
}
