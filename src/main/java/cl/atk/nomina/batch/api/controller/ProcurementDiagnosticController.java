package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.ProcurementDocumentDiagnosticRequest;
import cl.atk.nomina.batch.api.dto.ProcurementDocumentDiagnosticResponse;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.procurement.client.ProcurementClient;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.exception.ProcurementClientException;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.procurement.mapper.ProcurementDocumentMapper;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/dev/procurement/documents")
@Tag(name = "Procurement Diagnostic", description = "Endpoints temporales de diagnostico Procurement; no usar en produccion")
@ConditionalOnProperty(name = "app.diagnostics.enabled", havingValue = "true")
public class ProcurementDiagnosticController {

    private final NominaXmlParserService parserService;
    private final ProcurementDocumentMapper documentMapper;
    private final ProcurementClient procurementClient;

    public ProcurementDiagnosticController(
            NominaXmlParserService parserService,
            ProcurementDocumentMapper documentMapper,
            ProcurementClient procurementClient) {
        this.parserService = parserService;
        this.documentMapper = documentMapper;
        this.procurementClient = procurementClient;
    }

    @PostMapping("/test")
    @Operation(summary = "[DIAGNOSTIC - not for production] Mapea XML SOAP Artikos local/raw y envia un documento a Procurement")
    public ProcurementDocumentDiagnosticResponse testDocument(
            @Valid @RequestBody ProcurementDocumentDiagnosticRequest request) {
        ArtikosProfileType profileType = parseProfile(request.profile());
        Nomina nomina = request.hasRawXml()
                ? parserService.parseFromString(request.rawXml())
                : parserService.parseSampleFile();
        int documentIndex = request.resolvedDocumentIndex();
        DocumentoContable documento = documentAt(nomina, documentIndex);

        ProcurementDocumentRequest procurementRequest = documentMapper.toCmpDocumentRequest(
                profileType,
                nomina,
                documento);
        ProcurementDocumentPostResult procurementResult = procurementClient.postDocument(procurementRequest);

        return new ProcurementDocumentDiagnosticResponse(
                profileType.name(),
                nomina.cabecera() == null ? null : nomina.cabecera().numeroNomina(),
                documentIndex,
                documento.secuencia(),
                documento.idDocumento(),
                documento.numeroDocumento(),
                documento.rutProveedor(),
                documento.tipoDocumento(),
                true,
                procurementResult.successful(),
                procurementResult.statusCode(),
                procurementResult.successful() ? "OK" : "NOK",
                procurementResult.message(),
                procurementResult.errorMessage(),
                procurementResult.externalDocumentId());
    }

    private DocumentoContable documentAt(Nomina nomina, int documentIndex) {
        if (nomina.documentos() == null || nomina.documentos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nomina no contiene documentos");
        }
        if (documentIndex >= nomina.documentos().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "documentIndex fuera de rango. Documentos disponibles: " + nomina.documentos().size());
        }
        return nomina.documentos().get(documentIndex);
    }

    private ArtikosProfileType parseProfile(String profile) {
        try {
            return ArtikosProfileType.from(profile);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @ExceptionHandler(ProcurementMappingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMappingException(ProcurementMappingException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(ProcurementClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleClientException(ProcurementClientException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(NominaXmlParsingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleXmlParsingException(NominaXmlParsingException exception) {
        return exception.getMessage();
    }
}
