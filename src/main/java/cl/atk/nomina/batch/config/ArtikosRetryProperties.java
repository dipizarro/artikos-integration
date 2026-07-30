package cl.atk.nomina.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "artikos.retry")
public class ArtikosRetryProperties {

    private boolean enabled = true;
    private Integer maxAttempts = 3;
    private Integer backoffMs = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Integer getBackoffMs() {
        return backoffMs;
    }

    public void setBackoffMs(Integer backoffMs) {
        this.backoffMs = backoffMs;
    }

    public int resolvedMaxAttempts() {
        if (!enabled) {
            return 1;
        }
        return maxAttempts == null || maxAttempts < 1 ? 1 : maxAttempts;
    }

    public long resolvedBackoffMs() {
        return backoffMs == null || backoffMs < 0 ? 0L : backoffMs;
    }
}
