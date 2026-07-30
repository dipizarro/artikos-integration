package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import org.springframework.stereotype.Service;

@Service
public class NominaErrorPolicyService {

    private static final int CONTROL_ERROR_MAX_LENGTH = 500;

    public boolean shouldFailJob(IntegrationErrorType errorType) {
        return errorType != IntegrationErrorType.ARTIKOS_NO_NOMINAS;
    }

    public boolean shouldMarkControlNominaError(IntegrationErrorType errorType, Long numeroNomina) {
        if (numeroNomina == null) {
            return false;
        }
        return switch (errorType) {
            case XML_PARSING_ERROR,
                    NOMINA_CONFIRM_ERROR,
                    NOMINA_PROCESSING_ERROR,
                    PROCUREMENT_MAPPING_ERROR,
                    PROCUREMENT_TECHNICAL_ERROR,
                    NOMINA_RESULT_ERROR -> true;
            case ARTIKOS_FETCH_ERROR,
                    ARTIKOS_NO_NOMINAS,
                    ORACLE_CONTROL_ERROR,
                    UNKNOWN_ERROR -> false;
        };
    }

    public String buildControlErrorMessage(ArtikosIntegrationException exception) {
        StringBuilder message = new StringBuilder(exception.getErrorType().name());
        if (exception.getOperation() != null && !exception.getOperation().isBlank()) {
            message.append(" operation=").append(exception.getOperation());
        }
        String externalMessage = exception.getExternalMessage() == null
                ? exception.getMessage()
                : exception.getExternalMessage();
        if (externalMessage != null && !externalMessage.isBlank()) {
            message.append(" message=").append(externalMessage);
        }
        return StringSanitizer.compactAndTruncate(message.toString(), CONTROL_ERROR_MAX_LENGTH);
    }
}
