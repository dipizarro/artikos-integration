package cl.atk.nomina.batch.procurement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "procurement.client")
public class ProcurementClientProperties {

    private String baseUrl;
    private String documentPath = "/api/v1/document";
    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 30000;
    private Boolean enabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public int resolvedConnectTimeoutMs() {
        return positiveOrDefault(connectTimeoutMs, 5000);
    }

    public int resolvedReadTimeoutMs() {
        return positiveOrDefault(readTimeoutMs, 30000);
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}
