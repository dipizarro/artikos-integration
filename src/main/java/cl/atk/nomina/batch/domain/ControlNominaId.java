package cl.atk.nomina.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ControlNominaId implements Serializable {

    @Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

    @Column(name = "NUMERO_NOMINA", nullable = false)
    private Long numeroNomina;

    public ControlNominaId() {
    }

    public ControlNominaId(Long jobExecutionId, Long numeroNomina) {
        this.jobExecutionId = jobExecutionId;
        this.numeroNomina = numeroNomina;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public Long getNumeroNomina() {
        return numeroNomina;
    }

    public void setNumeroNomina(Long numeroNomina) {
        this.numeroNomina = numeroNomina;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ControlNominaId that)) {
            return false;
        }
        return Objects.equals(jobExecutionId, that.jobExecutionId)
                && Objects.equals(numeroNomina, that.numeroNomina);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobExecutionId, numeroNomina);
    }
}
