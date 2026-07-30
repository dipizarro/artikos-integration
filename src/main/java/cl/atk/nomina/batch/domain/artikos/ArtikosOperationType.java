package cl.atk.nomina.batch.domain.artikos;

public enum ArtikosOperationType {
    CONSUMO_NOMINA("consumoNomina", "NOMFACTERP"),
    RESPUESTA_NOMINA("respuestaNomina", "NOMFACTCONFIR"),
    RESULTADO_NOMINA("resultadoNomina", "NOMFACTRES");

    private final String propertyName;
    private final String expectedMsgCode;

    ArtikosOperationType(String propertyName, String expectedMsgCode) {
        this.propertyName = propertyName;
        this.expectedMsgCode = expectedMsgCode;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getExpectedMsgCode() {
        return expectedMsgCode;
    }
}
