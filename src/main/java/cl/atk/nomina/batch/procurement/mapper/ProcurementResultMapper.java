package cl.atk.nomina.batch.procurement.mapper;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.service.ProcurementDuplicateDetector;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import org.springframework.stereotype.Component;

@Component
public class ProcurementResultMapper {

    private static final int MESSAGE_MAX_LENGTH = 300;
    private static final String OK_MESSAGE = "Documento procesado correctamente en Procurement";
    public static final String IDEMPOTENT_OK_MESSAGE = "Documento ya existia en Procurement/ASI";
    private static final String NOK_DEFAULT_MESSAGE = "Documento rechazado por Procurement";

    private final ProcurementDuplicateDetector duplicateDetector;

    public ProcurementResultMapper(ProcurementDuplicateDetector duplicateDetector) {
        this.duplicateDetector = duplicateDetector;
    }

    public ResultadoDocumento toResultadoDocumento(
            Long numeroNomina,
            DocumentoContable documento,
            ProcurementDocumentPostResult procurementResult) {
        boolean duplicate = isDuplicate(procurementResult);
        boolean ok = procurementResult.successful() || duplicate;
        return new ResultadoDocumento(
                new SimulatedDocumentoContable(
                        documento,
                        1,
                        "%d-%d".formatted(numeroNomina, documento.idDocumento()),
                        numeroNomina),
                ok ? "OK" : "NOK",
                okMessage(procurementResult, duplicate),
                documento.idDocumento() == null ? null : documento.idDocumento().toString(),
                documento.rutProveedor(),
                documento.tipoErp(),
                documento.montoTotal());
    }

    public boolean isDuplicate(ProcurementDocumentPostResult procurementResult) {
        return procurementResult != null
                && duplicateDetector.isDuplicate(
                        procurementResult.statusCode(),
                        messageForDetection(procurementResult),
                        procurementResult.rawResponse());
    }

    private String okMessage(ProcurementDocumentPostResult procurementResult, boolean duplicate) {
        if (duplicate) {
            return IDEMPOTENT_OK_MESSAGE;
        }
        return procurementResult.successful() ? OK_MESSAGE : nokMessage(procurementResult);
    }

    private String messageForDetection(ProcurementDocumentPostResult procurementResult) {
        return "%s %s".formatted(procurementResult.message(), procurementResult.errorMessage());
    }

    private String nokMessage(ProcurementDocumentPostResult procurementResult) {
        String message = procurementResult.errorMessage();
        if (message == null || message.isBlank()) {
            message = procurementResult.message();
        }
        if (message == null || message.isBlank()) {
            message = NOK_DEFAULT_MESSAGE;
        }
        return StringSanitizer.compactAndTruncate(message, MESSAGE_MAX_LENGTH);
    }
}
