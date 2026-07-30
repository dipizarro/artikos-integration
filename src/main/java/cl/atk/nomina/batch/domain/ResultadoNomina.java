package cl.atk.nomina.batch.domain;

import java.util.List;

public record ResultadoNomina(
        Long jobExecutionId,
        Long numeroNomina,
        Integer totalDocuments,
        Integer totalOk,
        Integer totalNok,
        Integer totalConciliaciones,
        Integer totalDistribuciones,
        List<ResultadoDocumento> documentos,
        String nomfactresXml,
        String status,
        String errorMessage,
        String codEmpres) {

    public ResultadoNomina withJobExecutionId(Long newJobExecutionId) {
        return new ResultadoNomina(
                newJobExecutionId,
                numeroNomina,
                totalDocuments,
                totalOk,
                totalNok,
                totalConciliaciones,
                totalDistribuciones,
                documentos,
                nomfactresXml,
                status,
                errorMessage,
                codEmpres);
    }
}
