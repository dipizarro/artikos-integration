package cl.atk.nomina.batch.procurement.service;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.DocumentProcessingService;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "procurement.integration.enabled", havingValue = "true")
public class ProcurementDocumentProcessingService implements DocumentProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcurementDocumentProcessingService.class);
    private static final int DOCUMENT_ERROR_MESSAGE_MAX_LENGTH = 300;

    private final ProcurementIntegrationService integrationService;

    public ProcurementDocumentProcessingService(ProcurementIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @Override
    public List<ResultadoDocumento> processDocuments(ArtikosProfileType profile, Nomina nomina) {
        List<ResultadoDocumento> resultados = new ArrayList<>();
        Long numeroNomina = nomina.cabecera() == null ? null : nomina.cabecera().numeroNomina();
        for (DocumentoContable documento : nomina.documentos()) {
            try {
                resultados.add(integrationService.processDocument(profile, nomina, documento));
            } catch (ArtikosIntegrationException exception) {
                LOGGER.warn("Procurement document processing failed, continuing with next document profile={} "
                                + "numeroNomina={} secuencia={} idDocumento={} numeroDocumento={} errorType={} "
                                + "message={}",
                        profile,
                        numeroNomina,
                        documento.secuencia(),
                        documento.idDocumento(),
                        documento.numeroDocumento(),
                        exception.getErrorType(),
                        exception.getExternalMessage());
                resultados.add(failedResult(numeroNomina, documento, exception));
            }
        }
        return List.copyOf(resultados);
    }

    private ResultadoDocumento failedResult(
            Long numeroNomina,
            DocumentoContable documento,
            ArtikosIntegrationException exception) {
        return new ResultadoDocumento(
                new SimulatedDocumentoContable(
                        documento,
                        1,
                        "%s-%s".formatted(numeroNomina, documento.idDocumento()),
                        numeroNomina),
                "NOK",
                failureMessage(exception),
                documento.idDocumento() == null ? null : documento.idDocumento().toString(),
                documento.rutProveedor(),
                documento.tipoErp(),
                documento.montoTotal());
    }

    private String failureMessage(ArtikosIntegrationException exception) {
        String detail = exception.getExternalMessage() == null
                ? exception.getMessage()
                : exception.getExternalMessage();
        return StringSanitizer.compactAndTruncate(
                "%s: %s".formatted(exception.getErrorType().name(), detail),
                DOCUMENT_ERROR_MESSAGE_MAX_LENGTH);
    }
}
