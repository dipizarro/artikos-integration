package cl.atk.nomina.batch.config;

import cl.atk.nomina.batch.domain.artikos.ArtikosProfileConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationType;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "artikos.qa")
public class ArtikosProperties {

    private Endpoints endpoints = new Endpoints();
    private String soapAction = "";
    private String nominaSoapAction = "";
    private String connectorSoapAction = "";
    private Map<ArtikosProfileType, ArtikosProfileConfig> profiles = new EnumMap<>(ArtikosProfileType.class);

    public Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoints endpoints) {
        this.endpoints = endpoints == null ? new Endpoints() : endpoints;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public String getNominaSoapAction() {
        return nominaSoapAction;
    }

    public void setNominaSoapAction(String nominaSoapAction) {
        this.nominaSoapAction = nominaSoapAction;
    }

    public String getConnectorSoapAction() {
        return connectorSoapAction;
    }

    public void setConnectorSoapAction(String connectorSoapAction) {
        this.connectorSoapAction = connectorSoapAction;
    }

    public Map<ArtikosProfileType, ArtikosProfileConfig> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<ArtikosProfileType, ArtikosProfileConfig> profiles) {
        this.profiles = profiles == null ? new EnumMap<>(ArtikosProfileType.class) : profiles;
    }

    public ArtikosProfileConfig requireProfile(ArtikosProfileType profileType) {
        ArtikosProfileConfig profileConfig = profiles.get(profileType);
        if (profileConfig == null) {
            throw new IllegalStateException("No existe configuracion Artikos QA para el perfil " + profileType);
        }
        return profileConfig;
    }

    public ArtikosOperationConfig requireOperationConfig(
            ArtikosProfileType profileType,
            ArtikosOperationType operationType) {
        ArtikosProfileConfig profileConfig = requireProfile(profileType);
        ArtikosOperationConfig operationConfig = switch (operationType) {
            case CONSUMO_NOMINA -> profileConfig.getConsumoNomina();
            case RESPUESTA_NOMINA -> profileConfig.getRespuestaNomina();
            case RESULTADO_NOMINA -> profileConfig.getResultadoNomina();
        };
        if (operationConfig == null) {
            throw new IllegalStateException("No existe configuracion Artikos QA para el perfil "
                    + profileType + " y operacion " + operationType.getPropertyName());
        }
        return operationConfig;
    }

    public static class Endpoints {

        private String nominaUrl;
        private String connectorUrl;

        public String getNominaUrl() {
            return nominaUrl;
        }

        public void setNominaUrl(String nominaUrl) {
            this.nominaUrl = nominaUrl;
        }

        public String getConnectorUrl() {
            return connectorUrl;
        }

        public void setConnectorUrl(String connectorUrl) {
            this.connectorUrl = connectorUrl;
        }
    }
}
