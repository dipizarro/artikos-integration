package cl.atk.nomina.batch.procurement.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "procurement.mapping")
public class ProcurementMappingProperties {

    private final Hnr hnr = new Hnr();

    private String documentType = "CMP";
    private String codSistem = "CM";
    private BigDecimal valTipCambio = BigDecimal.ONE;
    private BigDecimal pctDscnto = BigDecimal.ZERO;
    private BigDecimal mtoDscnto = BigDecimal.ZERO;
    private BigDecimal pctIva = new BigDecimal("19");

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getCodSistem() {
        return codSistem;
    }

    public void setCodSistem(String codSistem) {
        this.codSistem = codSistem;
    }

    public BigDecimal getValTipCambio() {
        return valTipCambio;
    }

    public void setValTipCambio(BigDecimal valTipCambio) {
        this.valTipCambio = valTipCambio;
    }

    public BigDecimal getPctDscnto() {
        return pctDscnto;
    }

    public void setPctDscnto(BigDecimal pctDscnto) {
        this.pctDscnto = pctDscnto;
    }

    public BigDecimal getMtoDscnto() {
        return mtoDscnto;
    }

    public void setMtoDscnto(BigDecimal mtoDscnto) {
        this.mtoDscnto = mtoDscnto;
    }

    public BigDecimal getPctIva() {
        return pctIva;
    }

    public void setPctIva(BigDecimal pctIva) {
        this.pctIva = pctIva;
    }

    public Hnr getHnr() {
        return hnr;
    }

    public static class Hnr {
        private String codSistem = "HO";
        private String codImpsto = "RET";
        // Provisional development value; never used to calculate amounts.
        private BigDecimal pctRetn = new BigDecimal("12.25");

        public String getCodSistem() { return codSistem; }
        public void setCodSistem(String codSistem) { this.codSistem = codSistem; }
        public String getCodImpsto() { return codImpsto; }
        public void setCodImpsto(String codImpsto) { this.codImpsto = codImpsto; }
        public BigDecimal getPctRetn() { return pctRetn; }
        public void setPctRetn(BigDecimal pctRetn) { this.pctRetn = pctRetn; }
    }
}
