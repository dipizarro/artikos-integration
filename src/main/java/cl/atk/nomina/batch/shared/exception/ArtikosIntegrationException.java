package cl.atk.nomina.batch.shared.exception;

import cl.atk.nomina.batch.domain.error.IntegrationErrorType;

public class ArtikosIntegrationException extends RuntimeException {

    private final IntegrationErrorType errorType;
    private final String profile;
    private final Long numeroNomina;
    private final String operation;
    private final String externalMessage;

    public ArtikosIntegrationException(String message) {
        this(IntegrationErrorType.UNKNOWN_ERROR, message);
    }

    public ArtikosIntegrationException(String message, Throwable cause) {
        this(IntegrationErrorType.UNKNOWN_ERROR, null, null, null, message, cause);
    }

    public ArtikosIntegrationException(IntegrationErrorType errorType, String message) {
        this(errorType, null, null, null, message, null);
    }

    public ArtikosIntegrationException(
            IntegrationErrorType errorType,
            String profile,
            Long numeroNomina,
            String operation,
            String externalMessage,
            Throwable cause) {
        super(buildMessage(errorType, externalMessage), cause);
        this.errorType = errorType == null ? IntegrationErrorType.UNKNOWN_ERROR : errorType;
        this.profile = profile;
        this.numeroNomina = numeroNomina;
        this.operation = operation;
        this.externalMessage = externalMessage;
    }

    public IntegrationErrorType getErrorType() {
        return errorType;
    }

    public String getProfile() {
        return profile;
    }

    public Long getNumeroNomina() {
        return numeroNomina;
    }

    public String getOperation() {
        return operation;
    }

    public String getExternalMessage() {
        return externalMessage;
    }

    private static String buildMessage(IntegrationErrorType errorType, String externalMessage) {
        IntegrationErrorType resolvedType = errorType == null ? IntegrationErrorType.UNKNOWN_ERROR : errorType;
        if (externalMessage == null || externalMessage.isBlank()) {
            return resolvedType.name();
        }
        return resolvedType.name() + ": " + externalMessage;
    }
}
