package cl.atk.nomina.batch.procurement.lookup;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "GRL_MAE_ITEM_DET")
public class GrlMaeItemDetEntity {

    @EmbeddedId
    private GrlMaeItemDetId id;

    @Column(name = "COD_TIP_UNID", columnDefinition = "CHAR")
    private String codTipUnid;

    @Column(name = "COD_TIP_CNTA_ITEMS", columnDefinition = "CHAR")
    private String codTipCntaItems;

    @Column(name = "COD_CONTBL", columnDefinition = "CHAR")
    private String codContbl;

    @Column(name = "A_IND_VIGE", columnDefinition = "CHAR")
    private String aIndVige;

    public GrlMaeItemDetId getId() {
        return id;
    }

    public void setId(GrlMaeItemDetId id) {
        this.id = id;
    }

    public String getCodTipUnid() {
        return codTipUnid;
    }

    public void setCodTipUnid(String codTipUnid) {
        this.codTipUnid = codTipUnid;
    }

    public String getCodTipCntaItems() {
        return codTipCntaItems;
    }

    public void setCodTipCntaItems(String codTipCntaItems) {
        this.codTipCntaItems = codTipCntaItems;
    }

    public String getCodContbl() {
        return codContbl;
    }

    public void setCodContbl(String codContbl) {
        this.codContbl = codContbl;
    }

    public String getAIndVige() {
        return aIndVige;
    }

    public void setAIndVige(String aIndVige) {
        this.aIndVige = aIndVige;
    }
}
