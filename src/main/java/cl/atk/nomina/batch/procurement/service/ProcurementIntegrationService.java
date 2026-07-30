package cl.atk.nomina.batch.procurement.service;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.procurement.client.ProcurementClient;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.exception.ProcurementClientException;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.procurement.mapper.ProcurementDocumentMapper;
import cl.atk.nomina.batch.procurement.mapper.ProcurementResultMapper;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcurementIntegrationService {

    public static final String OPERATION = "PROCUREMENT_POST_DOCUMENT";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcurementIntegrationService.class);

    private final ProcurementDocumentMapper documentMapper;
    private final ProcurementClient procurementClient;
    private final ProcurementResultMapper resultMapper;

    public ProcurementIntegrationService(
            ProcurementDocumentMapper documentMapper,
            ProcurementClient procurementClient,
            ProcurementResultMapper resultMapper) {
        this.documentMapper = documentMapper;
        this.procurementClient = procurementClient;
        this.resultMapper = resultMapper;
    }

    public ResultadoDocumento processDocument(
            ArtikosProfileType profile,
            Nomina nomina,
            DocumentoContable documento) {
        Long numeroNomina = nomina.cabecera() == null ? null : nomina.cabecera().numeroNomina();
        long startedAt = System.nanoTime();
        LoggingContext.putOperation(OPERATION);
        try {
            LOGGER.info("Starting Procurement document processing profile={} numeroNomina={} secuencia={} "
                            + "idDocumento={} numeroDocumento={} rutProveedor={}",
                    profile,
                    numeroNomina,
                    documento.secuencia(),
                    documento.idDocumento(),
                    documento.numeroDocumento(),
                    documento.rutProveedor());
            ProcurementDocumentRequest request = documentMapper.toCmpDocumentRequest(profile, nomina, documento);
            ProcurementDocumentPostResult postResult = procurementClient.postDocument(request);
            boolean duplicate = resultMapper.isDuplicate(postResult);
            if (duplicate) {
                LOGGER.info("Procurement duplicate detected, treating as idempotent OK profile={} numeroNomina={} "
                                + "secuencia={} idDocumento={} numeroDocumento={} procurementStatusCode={}",
                        profile,
                        numeroNomina,
                        documento.secuencia(),
                        documento.idDocumento(),
                        documento.numeroDocumento(),
                        postResult.statusCode());
            }
            ResultadoDocumento resultadoDocumento = resultMapper.toResultadoDocumento(numeroNomina, documento, postResult);
            LOGGER.info("Finished Procurement document processing profile={} numeroNomina={} secuencia={} "
                            + "idDocumento={} numeroDocumento={} procurementStatusCode={} status={} elapsedMs={}",
                    profile,
                    numeroNomina,
                    documento.secuencia(),
                    documento.idDocumento(),
                    documento.numeroDocumento(),
                    postResult.statusCode(),
                    resultadoDocumento.status(),
                    elapsedMs(startedAt));
            return resultadoDocumento;
        } catch (ProcurementMappingException exception) {
            throw integrationException(
                    IntegrationErrorType.PROCUREMENT_MAPPING_ERROR,
                    profile,
                    numeroNomina,
                    exception.getMessage(),
                    exception);
        } catch (ProcurementClientException exception) {
            throw integrationException(
                    IntegrationErrorType.PROCUREMENT_TECHNICAL_ERROR,
                    profile,
                    numeroNomina,
                    exception.getMessage(),
                    exception);
        } finally {
            LoggingContext.clearOperation();
        }
    }

    private ArtikosIntegrationException integrationException(
            IntegrationErrorType errorType,
            ArtikosProfileType profile,
            Long numeroNomina,
            String message,
            RuntimeException cause) {
        LOGGER.warn("Procurement integration error errorType={} profile={} numeroNomina={} operation={} message={}",
                errorType, profile, numeroNomina, OPERATION, message);
        return new ArtikosIntegrationException(
                errorType,
                profile == null ? null : profile.name(),
                numeroNomina,
                OPERATION,
                message,
                cause);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
