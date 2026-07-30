package cl.atk.nomina.batch.procurement.mapper;

import static cl.atk.nomina.batch.shared.DeploymentFixVersion.FIX_VERSION;

import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ArtikosDocumentTypeMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtikosDocumentTypeMapper.class);

    private static final Map<String, String> DOCUMENT_TYPES = Map.ofEntries(
            Map.entry("33", "FEC"),
            Map.entry("34", "FCE"),
            Map.entry("56", "NDC"),
            Map.entry("61", "ECC"),
            Map.entry("NCC", "NCC"),
            Map.entry("FEC", "FEC"),
            Map.entry("FCE", "FCE"),
            Map.entry("FC", "FC"),
            Map.entry("FCP", "FCP"),
            Map.entry("EFE", "EFE"),
            Map.entry("END", "END"),
            Map.entry("OC", "OC"),
            Map.entry("NDC", "NDC"),
            Map.entry("ECC", "ECC"));

    public String toProcurementDocumentType(String tipoErp) {
        LOGGER.info("PROCUREMENT MAPPING CHECK - FIX_VERSION={} - tipoErp={}",
                FIX_VERSION, tipoErp);
        if (tipoErp == null || tipoErp.isBlank()) {
            throw new ProcurementMappingException("Missing Artikos field for Procurement mapping: DocumentoContable.tipoErp");
        }

        String normalized = tipoErp.trim().toUpperCase();
        String mapped = DOCUMENT_TYPES.get(normalized);
        if (mapped == null) {
            throw new ProcurementMappingException("Unsupported Artikos Tipo_ERP for Procurement mapping: " + tipoErp);
        }
        return mapped;
    }
}
