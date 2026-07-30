package cl.atk.nomina.batch.domain.artikos;

public class ArtikosProfileConfig {

    private ArtikosOperationConfig consumoNomina;
    private ArtikosOperationConfig respuestaNomina;
    private ArtikosOperationConfig resultadoNomina;

    public ArtikosOperationConfig getConsumoNomina() {
        return consumoNomina;
    }

    public void setConsumoNomina(ArtikosOperationConfig consumoNomina) {
        this.consumoNomina = consumoNomina;
    }

    public ArtikosOperationConfig getRespuestaNomina() {
        return respuestaNomina;
    }

    public void setRespuestaNomina(ArtikosOperationConfig respuestaNomina) {
        this.respuestaNomina = respuestaNomina;
    }

    public ArtikosOperationConfig getResultadoNomina() {
        return resultadoNomina;
    }

    public void setResultadoNomina(ArtikosOperationConfig resultadoNomina) {
        this.resultadoNomina = resultadoNomina;
    }
}
