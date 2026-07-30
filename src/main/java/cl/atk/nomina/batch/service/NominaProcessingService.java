package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NominaProcessingService {

    private final DocumentProcessingService documentProcessingService;
    private final SimulatedDocumentProcessingService simulatedDocumentProcessingService;
    private final NominaResultXmlService nominaResultXmlService;
    private final ControlNominaCompanyResolver companyResolver;

    public NominaProcessingService(
            DocumentProcessingService documentProcessingService,
            SimulatedDocumentProcessingService simulatedDocumentProcessingService,
            NominaResultXmlService nominaResultXmlService,
            ControlNominaCompanyResolver companyResolver) {
        this.documentProcessingService = documentProcessingService;
        this.simulatedDocumentProcessingService = simulatedDocumentProcessingService;
        this.nominaResultXmlService = nominaResultXmlService;
        this.companyResolver = companyResolver;
    }

    public ResultadoNomina process(
            Long jobExecutionId,
            Long numeroNomina,
            ArtikosProfileType profile,
            Nomina nomina,
            ArtikosOperationConfig resultadoOperationConfig) {
        return processWithDocumentService(
                jobExecutionId,
                numeroNomina,
                profile,
                nomina,
                resultadoOperationConfig,
                documentProcessingService);
    }

    public ResultadoNomina processSimulated(
            Long jobExecutionId,
            Long numeroNomina,
            ArtikosProfileType profile,
            Nomina nomina,
            ArtikosOperationConfig resultadoOperationConfig) {
        return processWithDocumentService(
                jobExecutionId,
                numeroNomina,
                profile,
                nomina,
                resultadoOperationConfig,
                simulatedDocumentProcessingService);
    }

    public ResultadoNomina processAlreadyOk(
            Long jobExecutionId,
            Long numeroNomina,
            ArtikosProfileType profile,
            Nomina nomina,
            ArtikosOperationConfig resultadoOperationConfig) {
        List<ResultadoDocumento> documentos = new ArrayList<>();
        nomina.documentos().forEach(documento -> documentos.add(new ResultadoDocumento(
                new SimulatedDocumentoContable(
                        documento,
                        1,
                        "%d-%d".formatted(numeroNomina, documento.idDocumento()),
                        numeroNomina),
                "OK",
                "Documento omitido: nomina ya procesada OK",
                documento.idDocumento() == null ? null : documento.idDocumento().toString(),
                documento.rutProveedor(),
                documento.tipoErp(),
                documento.montoTotal())));

        return buildResultadoNomina(
                jobExecutionId,
                numeroNomina,
                nomina,
                resultadoOperationConfig,
                List.copyOf(documentos),
                null,
                companyResolver.resolveCodEmpres(profile, nomina));
    }

    private ResultadoNomina processWithDocumentService(
            Long jobExecutionId,
            Long numeroNomina,
            ArtikosProfileType profile,
            Nomina nomina,
            ArtikosOperationConfig resultadoOperationConfig,
            DocumentProcessingService processingService) {
        List<ResultadoDocumento> documentos = processingService.processDocuments(profile, nomina);

        return buildResultadoNomina(
                jobExecutionId,
                numeroNomina,
                nomina,
                resultadoOperationConfig,
                documentos,
                documentos.isEmpty() ? "Nomina sin documentos para informar" : null,
                companyResolver.resolveCodEmpres(profile, nomina));
    }

    private ResultadoNomina buildResultadoNomina(
            Long jobExecutionId,
            Long numeroNomina,
            Nomina nomina,
            ArtikosOperationConfig resultadoOperationConfig,
            List<ResultadoDocumento> documentos,
            String errorMessage,
            String codEmpres) {
        int totalOk = (int) documentos.stream().filter(ResultadoDocumento::isOk).count();
        int totalNok = documentos.size() - totalOk;
        int totalConciliaciones = nomina.documentos().stream()
                .mapToInt(documento -> documento.conciliaciones().size())
                .sum();
        int totalDistribuciones = nomina.documentos().stream()
                .flatMap(documento -> documento.conciliaciones().stream())
                .mapToInt(conciliacion -> conciliacion.distribuciones().size())
                .sum();
        String status = totalNok == 0 && !documentos.isEmpty() ? "OK" : "NOK";
        String resolvedErrorMessage = totalNok > 0 ? nokDocumentsMessage(documentos) : errorMessage;

        ResultadoNomina result = new ResultadoNomina(
                jobExecutionId,
                numeroNomina,
                documentos.size(),
                totalOk,
                totalNok,
                totalConciliaciones,
                totalDistribuciones,
                List.copyOf(documentos),
                "",
                status,
                resolvedErrorMessage,
                codEmpres);

        return new ResultadoNomina(
                result.jobExecutionId(),
                result.numeroNomina(),
                result.totalDocuments(),
                result.totalOk(),
                result.totalNok(),
                result.totalConciliaciones(),
                result.totalDistribuciones(),
                result.documentos(),
                nominaResultXmlService.buildNomfactresXml(result, resultadoOperationConfig),
                result.status(),
                result.errorMessage(),
                result.codEmpres());
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
        String idDocumento = resultadoDocumento.resolvedDocFolio();
        String numeroDocumento = resultadoDocumento.originalDocumento() == null
                ? ""
                : value(resultadoDocumento.originalDocumento().numeroDocumento());
        return "idDocumento=%s numeroDocumento=%s tipoErp=%s message=%s".formatted(
                value(idDocumento),
                numeroDocumento,
                value(resultadoDocumento.resolvedDocTipoDoc()),
                value(resultadoDocumento.message()));
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}
