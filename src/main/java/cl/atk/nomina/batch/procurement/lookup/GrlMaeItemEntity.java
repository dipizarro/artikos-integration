package cl.atk.nomina.batch.procurement.lookup;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "GRL_MAE_ITEM")
public class GrlMaeItemEntity {

    @EmbeddedId
    private GrlMaeItemId id;

    @Column(name = "A_IND_VIGE", columnDefinition = "CHAR")
    private String aIndVige;

    public GrlMaeItemId getId() {
        return id;
    }

    public void setId(GrlMaeItemId id) {
        this.id = id;
    }

    public String getAIndVige() {
        return aIndVige;
    }

    public void setAIndVige(String aIndVige) {
        this.aIndVige = aIndVige;
    }
}
