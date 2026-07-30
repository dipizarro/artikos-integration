package cl.atk.nomina.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "artikos.http")
public class ArtikosHttpProperties {

    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 30000;

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
