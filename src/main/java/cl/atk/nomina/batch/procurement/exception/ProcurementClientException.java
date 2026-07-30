package cl.atk.nomina.batch.procurement.exception;

public class ProcurementClientException extends RuntimeException {

    public ProcurementClientException(String message) {
        super(message);
    }

    public ProcurementClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
