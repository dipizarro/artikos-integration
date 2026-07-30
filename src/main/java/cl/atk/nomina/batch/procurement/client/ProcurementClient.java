package cl.atk.nomina.batch.procurement.client;

import cl.atk.nomina.batch.procurement.config.ProcurementClientProperties;
import cl.atk.nomina.batch.procurement.dto.ProcurementApiResponse;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.exception.ProcurementClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ProcurementClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcurementClient.class);

    private final ProcurementClientProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public ProcurementClient(
            ProcurementClientProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .requestFactory(requestFactory(properties))
                .build();
    }

    ProcurementClient(
            ProcurementClientProperties properties,
            ObjectMapper objectMapper,
            RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    public ProcurementDocumentPostResult postDocument(ProcurementDocumentRequest request) {
        validateConfiguration();
        String endpoint = documentEndpoint();
        String requestBody = serializeRequest(request);
        long startedAt = System.nanoTime();

        LOGGER.info("Calling Procurement document endpoint endpoint={} documentPath={}",
                sanitizeEndpoint(endpoint), properties.getDocumentPath());
        LOGGER.debug("Procurement document request json={}", requestBody);

        try {
            return restClient.post()
                    .uri(URI.create(endpoint))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .exchange((httpRequest, response) -> {
                        HttpStatusCode httpStatus = response.getStatusCode();
                        String responseBody = readBody(response.getBody());
                        LOGGER.info("Procurement document response httpStatus={} elapsedMs={}",
                                httpStatus.value(), elapsedMs(startedAt));

                        if (httpStatus.is5xxServerError()) {
                            throw technicalException(
                                    "Procurement respondio HTTP " + httpStatus.value(),
                                    httpStatus.value(),
                                    null,
                                    requestBody,
                                    responseBody,
                                    null);
                        }

                        ProcurementDocumentPostResult result = parseResult(httpStatus, responseBody, requestBody);
                        LOGGER.info("Procurement document functional response httpStatus={} statusCode={} "
                                        + "successful={} elapsedMs={}",
                                httpStatus.value(), result.statusCode(), result.successful(), elapsedMs(startedAt));
                        return result;
                    });
        } catch (ProcurementClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw technicalException(
                    "No fue posible consumir Procurement document endpoint: " + compact(exception.getMessage()),
                    null,
                    null,
                    requestBody,
                    null,
                    exception);
        }
    }

    private ProcurementDocumentPostResult parseResult(
            HttpStatusCode httpStatus,
            String responseBody,
            String requestBody) {
        ProcurementApiResponse<JsonNode> apiResponse = parseResponse(responseBody, httpStatus, requestBody);
        Integer functionalStatusCode = apiResponse.statusCode();
        boolean successful = httpStatus.is2xxSuccessful() && Integer.valueOf(0).equals(functionalStatusCode);
        String errorMessage = successful ? null : errorMessage(apiResponse);
        if (!successful && isTechnicalProcurementFailure(responseBody, errorMessage, apiResponse.message())) {
            throw technicalException(
                    "Procurement technical failure",
                    httpStatus.value(),
                    functionalStatusCode,
                    requestBody,
                    responseBody,
                    null);
        }
        return new ProcurementDocumentPostResult(
                successful,
                functionalStatusCode,
                apiResponse.message(),
                errorMessage,
                externalDocumentId(apiResponse.payload()),
                responseBody);
    }

    private ProcurementApiResponse<JsonNode> parseResponse(
            String responseBody,
            HttpStatusCode httpStatus,
            String requestBody) {
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(ProcurementApiResponse.class, JsonNode.class);
            return objectMapper.readValue(responseBody, type);
        } catch (JsonProcessingException exception) {
            throw technicalException(
                    "Procurement respondio HTTP " + httpStatus.value() + " con body no parseable",
                    httpStatus.value(),
                    null,
                    requestBody,
                    responseBody,
                    exception);
        }
    }

    private String serializeRequest(ProcurementDocumentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new ProcurementClientException("No fue posible serializar request Procurement", exception);
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new ProcurementClientException("Procurement client is disabled: procurement.client.enabled=false");
        }
        requireText(properties.getBaseUrl(), "procurement.client.base-url");
        requireText(properties.getDocumentPath(), "procurement.client.document-path");
        requirePositive(properties.getConnectTimeoutMs(), "procurement.client.connect-timeout-ms");
        requirePositive(properties.getReadTimeoutMs(), "procurement.client.read-timeout-ms");
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new ProcurementClientException("Missing procurement client property: " + propertyName);
        }
    }

    private void requirePositive(Integer value, String propertyName) {
        if (value == null || value <= 0) {
            throw new ProcurementClientException("Invalid procurement client property: " + propertyName);
        }
    }

    private String documentEndpoint() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        String documentPath = properties.getDocumentPath().startsWith("/")
                ? properties.getDocumentPath()
                : "/" + properties.getDocumentPath();
        return baseUrl + documentPath;
    }

    private String errorMessage(ProcurementApiResponse<JsonNode> apiResponse) {
        if (apiResponse.error() != null) {
            return apiResponse.error().toString();
        }
        return apiResponse.message();
    }

    private String externalDocumentId(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        for (String fieldName : new String[] {"externalDocumentId", "documentId", "id"}) {
            JsonNode value = payload.get(fieldName);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private boolean isTechnicalProcurementFailure(String responseBody, String errorMessage, String message) {
        String text = "%s %s %s".formatted(responseBody, errorMessage, message).toUpperCase();
        return text.contains("ORA-")
                || text.contains("SQL ERROR")
                || text.contains("SQLSTATE")
                || text.contains("CONSTRAINT")
                || text.contains("RESTRICC")
                || text.contains("INTEGRITY")
                || text.contains("INTEGRIDAD")
                || text.contains("HIBERNATE")
                || text.contains("JDBC");
    }

    private ProcurementClientException technicalException(
            String reason,
            Integer httpStatus,
            Integer statusCode,
            String requestBody,
            String responseBody,
            Throwable cause) {
        LOGGER.error("Procurement technical error reason={} httpStatus={} statusCode={} requestJson={} responseBody={}",
                reason,
                httpStatus,
                statusCode,
                requestBody,
                responseBody,
                cause);
        String message = "procurementError=" + reason;
        if (httpStatus != null) {
            message += " httpStatus=" + httpStatus;
        }
        if (statusCode != null) {
            message += " statusCode=" + statusCode;
        }
        if (responseBody != null && !responseBody.isBlank()) {
            message += " response=" + compact(responseBody);
        }
        return cause == null
                ? new ProcurementClientException(message)
                : new ProcurementClientException(message, cause);
    }

    private SimpleClientHttpRequestFactory requestFactory(ProcurementClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.resolvedConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.resolvedReadTimeoutMs()));
        return requestFactory;
    }

    private String readBody(java.io.InputStream body) throws IOException {
        return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String sanitizeEndpoint(String endpoint) {
        return endpoint == null ? "" : endpoint.replaceAll("(?i)(token|password|secret)=[^&]+", "$1=****");
    }

    private String compact(String body) {
        String compactBody = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        return compactBody.length() <= 2000 ? compactBody : compactBody.substring(0, 2000) + "...";
    }
}
