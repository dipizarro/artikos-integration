package cl.atk.nomina.batch.service.artikos;

import cl.atk.nomina.batch.config.ArtikosProperties;
import cl.atk.nomina.batch.config.ArtikosHttpProperties;
import cl.atk.nomina.batch.config.ArtikosRetryProperties;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperation;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationType;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.NominaResultXmlService;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ArtikosSoapClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtikosSoapClient.class);
    private static final String DEFAULT_EXTRACTOR_SOAP_ACTION = "\"AtkWs_DocExtractor/EjecutaTrx\"";
    private static final String DEFAULT_CONNECTOR_SOAP_ACTION = "\"AtkWs_DocConnectorB2B/EjecutaTrx\"";

    private final ArtikosProperties artikosProperties;
    private final ArtikosNominaSoapRequestBuilder requestBuilder;
    private final ArtikosConfirmacionSoapRequestBuilder confirmacionRequestBuilder;
    private final ArtikosResultadoSoapRequestBuilder resultadoRequestBuilder;
    private final NominaResultXmlService nominaResultXmlService;
    private final ArtikosRetryProperties retryProperties;
    private final RestClient restClient;

    @Autowired
    public ArtikosSoapClient(
            ArtikosProperties artikosProperties,
            ArtikosNominaSoapRequestBuilder requestBuilder,
            ArtikosConfirmacionSoapRequestBuilder confirmacionRequestBuilder,
            ArtikosResultadoSoapRequestBuilder resultadoRequestBuilder,
            NominaResultXmlService nominaResultXmlService,
            ArtikosHttpProperties httpProperties,
            ArtikosRetryProperties retryProperties,
            RestClient.Builder restClientBuilder) {
        this.artikosProperties = artikosProperties;
        this.requestBuilder = requestBuilder;
        this.confirmacionRequestBuilder = confirmacionRequestBuilder;
        this.resultadoRequestBuilder = resultadoRequestBuilder;
        this.nominaResultXmlService = nominaResultXmlService;
        this.retryProperties = retryProperties;
        this.restClient = restClientBuilder
                .requestFactory(requestFactory(httpProperties))
                .build();
    }

    ArtikosSoapClient(
            ArtikosProperties artikosProperties,
            ArtikosNominaSoapRequestBuilder requestBuilder,
            ArtikosConfirmacionSoapRequestBuilder confirmacionRequestBuilder,
            ArtikosResultadoSoapRequestBuilder resultadoRequestBuilder,
            NominaResultXmlService nominaResultXmlService,
            ArtikosRetryProperties retryProperties,
            RestClient restClient) {
        this.artikosProperties = artikosProperties;
        this.requestBuilder = requestBuilder;
        this.confirmacionRequestBuilder = confirmacionRequestBuilder;
        this.resultadoRequestBuilder = resultadoRequestBuilder;
        this.nominaResultXmlService = nominaResultXmlService;
        this.retryProperties = retryProperties;
        this.restClient = restClient;
    }

    public String fetchNominaRawXml(ArtikosProfileType profileType) {
        ArtikosOperationConfig operationConfig = artikosProperties.requireOperationConfig(
                profileType,
                ArtikosOperationType.CONSUMO_NOMINA);
        String endpoint = artikosProperties.getEndpoints().getNominaUrl();
        String requestXml = requestBuilder.buildNomfacterpRequest(operationConfig);

        long startedAt = System.nanoTime();
        LoggingContext.putProfile(profileType.name());
        LoggingContext.putOperation(ArtikosOperation.NOMFACTERP.name());
        try {
            logOperation(profileType, ArtikosOperation.NOMFACTERP, endpoint, operationConfig);
            LOGGER.debug("Artikos NOMFACTERP request profile={} xml={}",
                    profileType, requestBuilder.maskToken(requestXml));

            return postSoap(
                    endpoint,
                    requestXml,
                    resolveSoapAction(
                            artikosProperties.getNominaSoapAction(),
                            artikosProperties.getSoapAction(),
                            DEFAULT_EXTRACTOR_SOAP_ACTION),
                    "nomina",
                    profileType,
                    null);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (ArtikosSoapClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            LOGGER.warn("Artikos SOAP technical error operation={} profile={} endpoint={} elapsedMs={} "
                            + "exceptionClass={} exceptionMessage={}",
                    ArtikosOperation.NOMFACTERP,
                    profileType,
                    endpoint,
                    elapsedMs(startedAt),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception);
            throw new ArtikosSoapClientException("No fue posible consultar nominas en Artikos QA", exception);
        } finally {
            LoggingContext.clearOperation();
        }
    }

    public String confirmNominaRawXml(
            ArtikosProfileType profileType,
            Long numeroNomina,
            Integer estadoRespuesta) {
        ArtikosOperationConfig operationConfig = artikosProperties.requireOperationConfig(
                profileType,
                ArtikosOperationType.RESPUESTA_NOMINA);
        String endpoint = artikosProperties.getEndpoints().getConnectorUrl();
        String requestXml = confirmacionRequestBuilder.buildNomfactconfirRequest(
                operationConfig,
                numeroNomina,
                estadoRespuesta);

        long startedAt = System.nanoTime();
        LoggingContext.putProfile(profileType.name());
        LoggingContext.putNumeroNomina(numeroNomina);
        LoggingContext.putOperation(ArtikosOperation.NOMFACTCONFIR.name());
        try {
            logOperation(profileType, ArtikosOperation.NOMFACTCONFIR, endpoint, operationConfig);
            LOGGER.info("Artikos NOMFACTCONFIR request shape profile={} numeroNomina={} {}",
                    profileType, numeroNomina, confirmacionRequestBuilder.describeContractShape(requestXml));
            LOGGER.debug("Artikos NOMFACTCONFIR request profile={} numeroNomina={} xml={}",
                    profileType, numeroNomina, confirmacionRequestBuilder.maskToken(requestXml));

            return postSoap(
                    endpoint,
                    requestXml,
                    resolveSoapAction(
                            artikosProperties.getConnectorSoapAction(),
                            artikosProperties.getSoapAction(),
                            DEFAULT_CONNECTOR_SOAP_ACTION),
                    "confirmation",
                    profileType,
                    numeroNomina);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (ArtikosSoapClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            LOGGER.warn("Artikos SOAP technical error operation={} profile={} endpoint={} elapsedMs={} "
                            + "exceptionClass={} exceptionMessage={}",
                    ArtikosOperation.NOMFACTCONFIR,
                    profileType,
                    endpoint,
                    elapsedMs(startedAt),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception);
            throw new ArtikosSoapClientException("No fue posible confirmar recepcion de nomina en Artikos QA",
                    exception);
        } finally {
            LoggingContext.clearOperation();
        }
    }

    public ArtikosOperationConfig resultadoNominaConfig(ArtikosProfileType profileType) {
        return artikosProperties.requireOperationConfig(profileType, ArtikosOperationType.RESULTADO_NOMINA);
    }

    public String sendNominaResultRawXml(
            ArtikosProfileType profileType,
            ResultadoNomina resultadoNomina) {
        ArtikosOperationConfig operationConfig = resultadoNominaConfig(profileType);
        String endpoint = artikosProperties.getEndpoints().getConnectorUrl();
        String nomfactresXml = StringUtils.hasText(resultadoNomina.nomfactresXml())
                ? resultadoNomina.nomfactresXml()
                : nominaResultXmlService.buildNomfactresXml(resultadoNomina, operationConfig);
        String requestXml = resultadoRequestBuilder.buildNomfactresRequest(operationConfig, nomfactresXml);

        long startedAt = System.nanoTime();
        LoggingContext.putProfile(profileType.name());
        LoggingContext.putNumeroNomina(resultadoNomina.numeroNomina());
        LoggingContext.putOperation(ArtikosOperation.NOMFACTRES.name());
        try {
            logOperation(profileType, ArtikosOperation.NOMFACTRES, endpoint, operationConfig);
            LOGGER.info("Artikos NOMFACTRES request shape profile={} numeroNomina={} {}",
                    profileType, resultadoNomina.numeroNomina(), resultadoRequestBuilder.describeContractShape(requestXml));
            LOGGER.info("Artikos NOMFACTRES payload sent profile={} numeroNomina={} xml={}",
                    profileType, resultadoNomina.numeroNomina(), nomfactresXml);
            LOGGER.debug("Artikos NOMFACTRES request profile={} numeroNomina={} xml={}",
                    profileType, resultadoNomina.numeroNomina(), resultadoRequestBuilder.maskToken(requestXml));

            return postSoap(
                    endpoint,
                    requestXml,
                    resolveSoapAction(
                            artikosProperties.getConnectorSoapAction(),
                            artikosProperties.getSoapAction(),
                            DEFAULT_CONNECTOR_SOAP_ACTION),
                    "result",
                    profileType,
                    resultadoNomina.numeroNomina());
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (ArtikosSoapClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            LOGGER.warn("Artikos SOAP technical error operation={} profile={} endpoint={} elapsedMs={} "
                            + "exceptionClass={} exceptionMessage={}",
                    ArtikosOperation.NOMFACTRES,
                    profileType,
                    endpoint,
                    elapsedMs(startedAt),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception);
            throw new ArtikosSoapClientException("No fue posible enviar resultado de nomina en Artikos QA",
                    exception);
        } finally {
            LoggingContext.clearOperation();
        }
    }

    private String postSoap(
            String endpoint,
            String requestXml,
            String soapAction,
            String operation,
            ArtikosProfileType profileType,
            Long numeroNomina) {
        long startedAt = System.nanoTime();
        return executeWithRetry(operation, profileType, numeroNomina, () -> restClient.post()
                .uri(URI.create(endpoint))
                .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
                .headers(headers -> applySoapAction(headers, soapAction))
                .body(requestXml)
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    String responseBody = readBody(response.getBody());
                    LOGGER.info("Artikos QA {} SOAP response profile={} numeroNomina={} httpStatus={} elapsedMs={}",
                            operation, profileType, numeroNomina, statusCode, elapsedMs(startedAt));
                    if (statusCode.isError()) {
                        String safeBody = compact(responseBody);
                        LOGGER.warn("Artikos QA {} SOAP HTTP error profile={} numeroNomina={} httpStatus={} "
                                        + "elapsedMs={} body={}",
                                operation, profileType, numeroNomina, statusCode, elapsedMs(startedAt), safeBody);
                        throw new ArtikosSoapClientException(
                                "Artikos QA respondio HTTP " + statusCode.value() + ": " + safeBody,
                                null,
                                statusCode.is5xxServerError());
                    }
                    return responseBody;
                }));
    }

    private String executeWithRetry(
            String operation,
            ArtikosProfileType profileType,
            Long numeroNomina,
            Supplier<String> action) {
        int maxAttempts = retryProperties.resolvedMaxAttempts();
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.nanoTime();
            try {
                return action.get();
            } catch (ArtikosSoapClientException exception) {
                lastException = exception;
                if (!exception.isRetryable() || attempt >= maxAttempts) {
                    throw exception;
                }
                logRetry(operation, profileType, numeroNomina, attempt, maxAttempts, startedAt, exception);
                backoff();
            } catch (RestClientException exception) {
                lastException = exception;
                if (attempt >= maxAttempts) {
                    throw exception;
                }
                logRetry(operation, profileType, numeroNomina, attempt, maxAttempts, startedAt, exception);
                backoff();
            }
        }
        throw lastException == null
                ? new ArtikosSoapClientException("No fue posible ejecutar llamada SOAP Artikos")
                : lastException;
    }

    private void logRetry(
            String operation,
            ArtikosProfileType profileType,
            Long numeroNomina,
            int attempt,
            int maxAttempts,
            long startedAt,
            RuntimeException exception) {
        LOGGER.warn("Retrying Artikos SOAP technical error operation={} profile={} numeroNomina={} attempt={} "
                        + "maxAttempts={} elapsedMs={} exceptionClass={} exceptionMessage={}",
                operation,
                profileType,
                numeroNomina,
                attempt,
                maxAttempts,
                elapsedMs(startedAt),
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }

    private void backoff() {
        long backoffMs = retryProperties.resolvedBackoffMs();
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ArtikosSoapClientException("Retry Artikos interrumpido", exception);
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(ArtikosHttpProperties httpProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(httpProperties.resolvedConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(httpProperties.resolvedReadTimeoutMs()));
        return requestFactory;
    }

    private void applySoapAction(HttpHeaders headers, String soapAction) {
        headers.add("SOAPAction", soapAction);
    }

    private void logOperation(
            ArtikosProfileType profileType,
            ArtikosOperation operation,
            String endpoint,
            ArtikosOperationConfig operationConfig) {
        LOGGER.info("Calling Artikos QA SOAP endpoint profile={} operation={} endpoint={} msgCode={} "
                        + "msgFromAddress={} msgCodFromAddress={} msgToAddress={} msgCodSis={} "
                        + "tokenPresent={} tokenMasked={}",
                profileType,
                operation,
                endpoint,
                operationConfig.getMsgCode(),
                operationConfig.getMsgFromAddress(),
                operationConfig.getMsgCodFromAddress(),
                operationConfig.getMsgToAddress(),
                operationConfig.getMsgCodSis(),
                ArtikosTokenMasker.isPresent(operationConfig.getToken()),
                ArtikosTokenMasker.mask(operationConfig.getToken()));
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String resolveSoapAction(
            String configuredOperationSoapAction,
            String configuredLegacySoapAction,
            String defaultSoapAction) {
        if (StringUtils.hasText(configuredOperationSoapAction)) {
            return configuredOperationSoapAction;
        }
        if (StringUtils.hasText(configuredLegacySoapAction)) {
            return configuredLegacySoapAction;
        }
        return defaultSoapAction;
    }

    private String readBody(java.io.InputStream body) throws IOException {
        return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
    }

    private String compact(String body) {
        String compactBody = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        return compactBody.length() <= 2000 ? compactBody : compactBody.substring(0, 2000) + "...";
    }
}
