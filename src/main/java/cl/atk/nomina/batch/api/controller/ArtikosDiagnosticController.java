package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.ConfirmNominaRequest;
import cl.atk.nomina.batch.api.dto.ConfirmNominaResponse;
import cl.atk.nomina.batch.api.dto.FetchNominaRequest;
import cl.atk.nomina.batch.api.dto.FetchNominaResponse;
import cl.atk.nomina.batch.api.dto.SendNominaResultRequest;
import cl.atk.nomina.batch.api.dto.SendNominaResultResponse;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosGenericResponse;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.NominaResultXmlService;
import cl.atk.nomina.batch.service.artikos.ArtikosGenericSoapResponseParser;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClient;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClientException;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapResponseParser;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/dev/artikos/nominas")
@Tag(name = "Artikos QA Diagnostic", description = "Endpoints temporales de diagnostico SOAP QA; no usar en produccion")
@ConditionalOnProperty(name = "app.diagnostics.enabled", havingValue = "true")
public class ArtikosDiagnosticController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtikosDiagnosticController.class);

    private final ArtikosSoapClient soapClient;
    private final ArtikosSoapResponseParser responseParser;
    private final ArtikosGenericSoapResponseParser genericResponseParser;
    private final NominaResultXmlService nominaResultXmlService;

    public ArtikosDiagnosticController(
            ArtikosSoapClient soapClient,
            ArtikosSoapResponseParser responseParser,
            ArtikosGenericSoapResponseParser genericResponseParser,
            NominaResultXmlService nominaResultXmlService) {
        this.soapClient = soapClient;
        this.responseParser = responseParser;
        this.genericResponseParser = genericResponseParser;
        this.nominaResultXmlService = nominaResultXmlService;
    }

    @PostMapping("/fetch")
    @Operation(summary = "[DIAGNOSTIC - not for production] Consulta nominas disponibles en Artikos QA con NOMFACTERP")
    public FetchNominaResponse fetchNomina(@Valid @RequestBody FetchNominaRequest request) {
        ArtikosProfileType profileType = parseProfile(request.profile());
        String rawXml = soapClient.fetchNominaRawXml(profileType);

        if (responseParser.isNoNominasResponse(rawXml)) {
            LOGGER.info("Artikos QA returned no nominas for profile={}", profileType);
            return new FetchNominaResponse(
                    profileType.name(),
                    false,
                    null,
                    null,
                    0,
                    responseParser.extractNoNominasMessage(rawXml));
        }

        Optional<Nomina> nomina = responseParser.extractNomina(rawXml);
        if (nomina.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Artikos QA no retorno una nomina ni un mensaje de no disponibilidad");
        }

        Nomina parsedNomina = nomina.get();
        return new FetchNominaResponse(
                profileType.name(),
                true,
                parsedNomina.cabecera().numeroNomina(),
                parsedNomina.cabecera().tipoNomina(),
                parsedNomina.cabecera().cantidadDocumentos(),
                "Nomina recibida correctamente desde Artikos");
    }

    @PostMapping("/confirm")
    @Operation(summary = "[DIAGNOSTIC - not for production] Confirma recepcion de nomina en Artikos QA con NOMFACTCONFIR")
    public ConfirmNominaResponse confirmNomina(@Valid @RequestBody ConfirmNominaRequest request) {
        ArtikosProfileType profileType = parseProfile(request.profile());
        String rawXml = soapClient.confirmNominaRawXml(
                profileType,
                request.numeroNomina(),
                request.estadoRespuesta());
        ArtikosGenericResponse response = genericResponseParser.parseGenericResponse(rawXml);

        return new ConfirmNominaResponse(
                profileType.name(),
                request.numeroNomina(),
                response.success(),
                response.msgStatus(),
                response.success()
                        ? "Confirmacion enviada correctamente a Artikos"
                        : response.messageText());
    }

    @PostMapping("/result/test")
    @Operation(summary = "[DIAGNOSTIC - not for production] Envia resultado de procesamiento de nomina en Artikos QA con NOMFACTRES")
    public SendNominaResultResponse sendNominaResult(@Valid @RequestBody SendNominaResultRequest request) {
        ArtikosProfileType profileType = parseProfile(request.profile());
        ResultadoNomina resultadoNomina = buildDiagnosticResult(profileType, request);
        String rawXml = soapClient.sendNominaResultRawXml(profileType, resultadoNomina);
        ArtikosGenericResponse response = genericResponseParser.parseGenericResponse(rawXml);

        return new SendNominaResultResponse(
                profileType.name(),
                request.numeroNomina(),
                response.success(),
                response.msgStatus(),
                response.success()
                        ? "Resultado enviado correctamente a Artikos"
                        : response.messageText());
    }

    private ArtikosProfileType parseProfile(String profile) {
        try {
            return ArtikosProfileType.from(profile);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private ResultadoNomina buildDiagnosticResult(
            ArtikosProfileType profileType,
            SendNominaResultRequest request) {
        String status = request.normalizedDocEstado();
        ResultadoDocumento documento = new ResultadoDocumento(
                null,
                status,
                request.docDescEstado(),
                request.docFolio(),
                request.docRutProveedor(),
                request.docTipoDoc(),
                request.monto());
        int totalOk = "OK".equals(status) ? 1 : 0;
        int totalNok = "NOK".equals(status) ? 1 : 0;

        ResultadoNomina resultado = new ResultadoNomina(
                null,
                request.numeroNomina(),
                1,
                totalOk,
                totalNok,
                0,
                0,
                List.of(documento),
                "",
                totalNok == 0 ? "OK" : "NOK",
                null,
                null);

        String nomfactresXml = nominaResultXmlService.buildNomfactresXml(
                resultado,
                soapClient.resultadoNominaConfig(profileType));
        return new ResultadoNomina(
                resultado.jobExecutionId(),
                resultado.numeroNomina(),
                resultado.totalDocuments(),
                resultado.totalOk(),
                resultado.totalNok(),
                resultado.totalConciliaciones(),
                resultado.totalDistribuciones(),
                resultado.documentos(),
                nomfactresXml,
                resultado.status(),
                resultado.errorMessage(),
                resultado.codEmpres());
    }

    @ExceptionHandler(ArtikosSoapClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleSoapClientException(ArtikosSoapClientException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(NominaXmlParsingException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleParsingException(NominaXmlParsingException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleConfigurationException(IllegalStateException exception) {
        return exception.getMessage();
    }
}
