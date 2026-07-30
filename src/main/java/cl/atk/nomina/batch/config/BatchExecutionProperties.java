package cl.atk.nomina.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atk.batch")
public class BatchExecutionProperties {

    private Integer defaultMaxNominas = 50;
    private Integer maxNominasPerRun = 50;
    private Integer realChunkSize = 1;

    public Integer getDefaultMaxNominas() {
        return defaultMaxNominas;
    }

    public void setDefaultMaxNominas(Integer defaultMaxNominas) {
        this.defaultMaxNominas = defaultMaxNominas;
    }

    public Integer getMaxNominasPerRun() {
        return maxNominasPerRun;
    }

    public void setMaxNominasPerRun(Integer maxNominasPerRun) {
        this.maxNominasPerRun = maxNominasPerRun;
    }

    public Integer getRealChunkSize() {
        return realChunkSize;
    }

    public void setRealChunkSize(Integer realChunkSize) {
        this.realChunkSize = realChunkSize;
    }

    public int resolvedMaxNominasPerRun() {
        return maxNominasPerRun == null || maxNominasPerRun < 1 ? 50 : maxNominasPerRun;
    }

    public int resolvedDefaultMaxNominas() {
        int maxAllowed = resolvedMaxNominasPerRun();
        int configuredDefault = defaultMaxNominas == null || defaultMaxNominas < 1 ? maxAllowed : defaultMaxNominas;
        return Math.min(configuredDefault, maxAllowed);
    }

    public int resolvedRealChunkSize() {
        return realChunkSize == null || realChunkSize < 1 ? 1 : realChunkSize;
    }
}
