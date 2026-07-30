package cl.atk.nomina.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "CONTROL_NOMINA")
public class ControlNominaEntity {

    @EmbeddedId
    private ControlNominaId id;

    @Column(name = "TOTAL_DOCUMENTS")
    private Integer totalDocuments;

    @Column(name = "TOTAL_OK")
    private Integer totalOk;

    @Column(name = "TOTAL_NOK")
    private Integer totalNok;

    @Column(name = "TOTAL_CONCILIACIONES")
    private Integer totalConciliaciones;

    @Column(name = "TOTAL_DISTRIBUCIONES")
    private Integer totalDistribuciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private ControlNominaStatus status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "ERROR_MESSAGE", length = 500)
    private String errorMessage;

    @Column(name = "COD_EMPRES", length = 3, columnDefinition = "CHAR")
    private String codEmpres;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ControlNominaId getId() {
        return id;
    }

    public void setId(ControlNominaId id) {
        this.id = id;
    }

    public Long getJobExecutionId() {
        return id != null ? id.getJobExecutionId() : null;
    }

    public void setJobExecutionId(Long jobExecutionId) {
        ensureId().setJobExecutionId(jobExecutionId);
    }

    public Long getNumeroNomina() {
        return id != null ? id.getNumeroNomina() : null;
    }

    public void setNumeroNomina(Long numeroNomina) {
        ensureId().setNumeroNomina(numeroNomina);
    }

    public Integer getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Integer getTotalOk() {
        return totalOk;
    }

    public void setTotalOk(Integer totalOk) {
        this.totalOk = totalOk;
    }

    public Integer getTotalNok() {
        return totalNok;
    }

    public void setTotalNok(Integer totalNok) {
        this.totalNok = totalNok;
    }

    public Integer getTotalConciliaciones() {
        return totalConciliaciones;
    }

    public void setTotalConciliaciones(Integer totalConciliaciones) {
        this.totalConciliaciones = totalConciliaciones;
    }

    public Integer getTotalDistribuciones() {
        return totalDistribuciones;
    }

    public void setTotalDistribuciones(Integer totalDistribuciones) {
        this.totalDistribuciones = totalDistribuciones;
    }

    public ControlNominaStatus getStatus() {
        return status;
    }

    public void setStatus(ControlNominaStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCodEmpres() {
        return codEmpres;
    }

    public void setCodEmpres(String codEmpres) {
        this.codEmpres = codEmpres;
    }

    private ControlNominaId ensureId() {
        if (id == null) {
            id = new ControlNominaId();
        }
        return id;
    }
}
