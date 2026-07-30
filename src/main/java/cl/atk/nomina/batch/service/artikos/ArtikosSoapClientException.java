package cl.atk.nomina.batch.service.artikos;

public class ArtikosSoapClientException extends RuntimeException {

    private final boolean retryable;

    public ArtikosSoapClientException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public ArtikosSoapClientException(String message) {
        this(message, null, false);
    }

    public ArtikosSoapClientException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
