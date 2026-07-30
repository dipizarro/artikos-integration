package cl.atk.nomina.batch.procurement.lookup;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GrlMaeItemDetId implements Serializable {

    @Column(name = "COD_EMPRES", columnDefinition = "CHAR")
    private String codEmpres;

    @Column(name = "NUM_PERIODO")
    private Integer numPeriodo;

    @Column(name = "COD_SISTEM", columnDefinition = "CHAR")
    private String codSistem;

    @Column(name = "COD_MONEDA", columnDefinition = "CHAR")
    private String codMoneda;

    @Column(name = "COD_CUENTA")
    private Long codCuenta;

    @Column(name = "COD_IMPSTO", columnDefinition = "CHAR")
    private String codImpsto;

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

    public String getCodSistem() {
        return codSistem;
    }

    public void setCodSistem(String codSistem) {
        this.codSistem = codSistem;
    }

    public String getCodMoneda() {
        return codMoneda;
    }

    public void setCodMoneda(String codMoneda) {
        this.codMoneda = codMoneda;
    }

    public Long getCodCuenta() {
        return codCuenta;
    }

    public void setCodCuenta(Long codCuenta) {
        this.codCuenta = codCuenta;
    }

    public String getCodImpsto() {
        return codImpsto;
    }

    public void setCodImpsto(String codImpsto) {
        this.codImpsto = codImpsto;
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
        if (!(object instanceof GrlMaeItemDetId that)) {
            return false;
        }
        return Objects.equals(codEmpres, that.codEmpres)
                && Objects.equals(numPeriodo, that.numPeriodo)
                && Objects.equals(codSistem, that.codSistem)
                && Objects.equals(codMoneda, that.codMoneda)
                && Objects.equals(codCuenta, that.codCuenta)
                && Objects.equals(codImpsto, that.codImpsto)
                && Objects.equals(grlCodItem, that.grlCodItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codEmpres, numPeriodo, codSistem, codMoneda, codCuenta, codImpsto, grlCodItem);
    }
}
