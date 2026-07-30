package cl.atk.nomina.batch.procurement.lookup;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GrlMaeItemId implements Serializable {

    @Column(name = "COD_EMPRES", columnDefinition = "CHAR")
    private String codEmpres;

    @Column(name = "NUM_PERIODO")
    private Integer numPeriodo;

    @Column(name = "GRL_COD_ITEM", columnDefinition = "CHAR")
    private String grlCodItem;

    public String getCodEmpres() {
        return codEmpres;
    }

    public void setCodEmpres(String codEmpres) {
        this.codEmpres = codEmpres;
    }

    public Integer getNumPeriodo() {
        return numPeriodo;
    }

    public void setNumPeriodo(Integer numPeriodo) {
        this.numPeriodo = numPeriodo;
    }

    public String getGrlCodItem() {
        return grlCodItem;
    }

    public void setGrlCodItem(String grlCodItem) {
        this.grlCodItem = grlCodItem;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof GrlMaeItemId that)) {
            return false;
        }
        return Objects.equals(codEmpres, that.codEmpres)
                && Objects.equals(numPeriodo, that.numPeriodo)
                && Objects.equals(grlCodItem, that.grlCodItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codEmpres, numPeriodo, grlCodItem);
    }
}
