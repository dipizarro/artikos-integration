package cl.atk.nomina.batch.shared.exception;

public class NominaXmlParsingException extends RuntimeException {

    public NominaXmlParsingException(String message) {
        super(message);
    }

    public NominaXmlParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
