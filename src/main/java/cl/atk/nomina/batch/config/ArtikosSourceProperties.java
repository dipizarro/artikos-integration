package cl.atk.nomina.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "artikos.source")
public class ArtikosSourceProperties {

    public static final String MODE_REMOTE = "remote";
    public static final String MODE_LOCAL_XML = "local-xml";

    private String mode = MODE_REMOTE;
    private String localXmlPath = "";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode == null || mode.isBlank() ? MODE_REMOTE : mode;
    }

    public String getLocalXmlPath() {
        return localXmlPath;
    }

    public void setLocalXmlPath(String localXmlPath) {
        this.localXmlPath = localXmlPath;
    }

    public boolean isLocalXmlMode() {
        return MODE_LOCAL_XML.equalsIgnoreCase(mode);
    }

    public boolean isRemoteMode() {
        return MODE_REMOTE.equalsIgnoreCase(mode);
    }
}
