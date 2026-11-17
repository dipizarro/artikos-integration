package cl.atk.nomina.batch.procurement.mapper;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.procurement.domain.ProcurementDocumentFamily;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ArtikosDocumentFamilyResolver {

    private static final Map<String, ProcurementDocumentFamily> DOCUMENT_FAMILIES = Map.of(
            "33", ProcurementDocumentFamily.CMP,
            "34", ProcurementDocumentFamily.CMP,
            "56", ProcurementDocumentFamily.CMP,
            "61", ProcurementDocumentFamily.CMP,
            // Preliminary HNR catalog; validate against a real HNR nomina in issue #32.
            "885", ProcurementDocumentFamily.HNR,
            "886", ProcurementDocumentFamily.HNR);

    public ProcurementDocumentFamily resolve(DocumentoContable documento) {
        if (documento == null) {
            throw new ProcurementMappingException("DocumentoContable is required for Procurement family mapping");
        }

        String tipoDocumento = documento.tipoDocumento();
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new ProcurementMappingException(
                    "Missing Artikos field for Procurement family mapping: DocumentoContable.tipoDocumento");
        }

        ProcurementDocumentFamily family = DOCUMENT_FAMILIES.get(tipoDocumento.trim());
        if (family == null) {
            throw new ProcurementMappingException(
                    "Unsupported Artikos Tipo_Documento for Procurement family mapping: " + tipoDocumento);
        }
        return family;
    }
}
