package cl.atk.nomina.batch.batch.processor;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.domain.SimulatedNomina;
import cl.atk.nomina.batch.service.ControlNominaService;
import cl.atk.nomina.batch.service.NominaResultXmlService;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class NominaItemProcessor implements ItemProcessor<SimulatedNomina, ResultadoNomina> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NominaItemProcessor.class);

    private final NominaDocumentoItemProcessor documentoItemProcessor;
    private final NominaResultXmlService nominaResultXmlService;
    private final ControlNominaService controlNominaService;
    private final Long jobExecutionId;

    public NominaItemProcessor(
            NominaDocumentoItemProcessor documentoItemProcessor,
            NominaResultXmlService nominaResultXmlService,
            ControlNominaService controlNominaService,
            Long jobExecutionId) {
        this.documentoItemProcessor = documentoItemProcessor;
        this.nominaResultXmlService = nominaResultXmlService;
        this.controlNominaService = controlNominaService;
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public ResultadoNomina process(SimulatedNomina item) throws Exception {
        Long numeroNomina = item.simulatedNumeroNomina();
        try {
            LOGGER.info("[CONTROL_NOMINA] PROCESSING jobExecutionId={} numeroNomina={}",
                    jobExecutionId, numeroNomina);
            controlNominaService.markProcessing(jobExecutionId, numeroNomina);
            return processNomina(item);
        } catch (RuntimeException exception) {
            markErrorSafely(numeroNomina, exception);
            throw exception;
        } catch (Exception exception) {
            markErrorSafely(numeroNomina, exception);
            return errorResult(item, exception);
        }
    }

    private ResultadoNomina processNomina(SimulatedNomina item) throws Exception {
        List<ResultadoDocumento> documentos = new ArrayList<>();
        for (DocumentoContable documento : item.documentos()) {
            documentos.add(documentoItemProcessor.process(new SimulatedDocumentoContable(
                    documento,
                    item.simulationIndex(),
                    simulatedDocumentKey(item, documento),
                    item.simulatedNumeroNomina())));
        }

        int totalOk = (int) documentos.stream().filter(ResultadoDocumento::isOk).count();
        int totalNok = documentos.size() - totalOk;
        int totalConciliaciones = item.documentos().stream()
                .mapToInt(documento -> documento.conciliaciones().size())
                .sum();
        int totalDistribuciones = item.documentos().stream()
                .flatMap(documento -> documento.conciliaciones().stream())
                .mapToInt(conciliacion -> conciliacion.distribuciones().size())
                .sum();
        String status = totalNok == 0 ? "OK" : "NOK";

        ResultadoNomina result = new ResultadoNomina(
                jobExecutionId,
                item.simulatedNumeroNomina(),
                documentos.size(),
                totalOk,
                totalNok,
                totalConciliaciones,
                totalDistribuciones,
                List.copyOf(documentos),
                "",
                status,
                null,
                null);

        return new ResultadoNomina(
                result.jobExecutionId(),
                result.numeroNomina(),
                result.totalDocuments(),
                result.totalOk(),
                result.totalNok(),
                result.totalConciliaciones(),
                result.totalDistribuciones(),
                result.documentos(),
                nominaResultXmlService.buildNomfactresXml(result),
                result.status(),
                totalNok > 0 ? nokDocumentsMessage(documentos) : result.errorMessage(),
                result.codEmpres());
    }

    private ResultadoNomina errorResult(SimulatedNomina item, Exception exception) {
        int totalDocuments = item.documentos().size();
        return new ResultadoNomina(
                jobExecutionId,
                item.simulatedNumeroNomina(),
                totalDocuments,
                0,
                totalDocuments,
                0,
                0,
                List.of(),
                "",
                "ERROR",
                exception.getMessage(),
                null);
    }

    private void markErrorSafely(Long numeroNomina, Exception exception) {
        try {
            LOGGER.error("[CONTROL_NOMINA] ERROR jobExecutionId={} numeroNomina={} error={}",
                    jobExecutionId, numeroNomina, exception.getMessage());
            controlNominaService.markError(jobExecutionId, numeroNomina, exception.getMessage());
        } catch (RuntimeException markErrorException) {
            LOGGER.error("Could not mark CONTROL_NOMINA ERROR for jobExecutionId={} numeroNomina={}",
                    jobExecutionId, numeroNomina, markErrorException);
        }
    }

    private String simulatedDocumentKey(SimulatedNomina nomina, DocumentoContable documento) {
        return "%d-%d-%03d".formatted(
                nomina.simulatedNumeroNomina(),
                documento.idDocumento(),
                nomina.simulationIndex());
    }

    private String nokDocumentsMessage(List<ResultadoDocumento> documentos) {
        List<ResultadoDocumento> nokDocuments = documentos.stream()
                .filter(documento -> !documento.isOk())
                .toList();
        String details = nokDocuments.stream()
                .map(this::nokDocumentMessage)
                .reduce((left, right) -> left + "; " + right)
                .orElse("sin detalle");
        return StringSanitizer.compactAndTruncate(
                "Procurement NOK documents count=%d: %s".formatted(nokDocuments.size(), details),
                500);
    }

    private String nokDocumentMessage(ResultadoDocumento resultadoDocumento) {
        String numeroDocumento = resultadoDocumento.originalDocumento() == null
                ? ""
                : value(resultadoDocumento.originalDocumento().numeroDocumento());
        return "idDocumento=%s numeroDocumento=%s tipoErp=%s message=%s".formatted(
                value(resultadoDocumento.resolvedDocFolio()),
                value(numeroDocumento),
                value(resultadoDocumento.resolvedDocTipoDoc()),
                value(resultadoDocumento.message()));
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}
