package cl.atk.nomina.batch.shared.exception;

public class BatchConcurrencyException extends RuntimeException {

    public BatchConcurrencyException(String message) {
        super(message);
    }
}
