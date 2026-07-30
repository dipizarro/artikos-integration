package cl.atk.nomina.batch.procurement.mapper;

import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;

public final class RutUtils {

    private RutUtils() {
    }

    public static Long extractRutNumber(String rutWithDv) {
        if (rutWithDv == null || rutWithDv.isBlank()) {
            throw new ProcurementMappingException("Rut proveedor is required for Procurement mapping");
        }

        String[] parts = normalize(rutWithDv).split("-");
        if (parts.length == 0 || parts[0].isBlank()) {
            throw new ProcurementMappingException("Invalid rut proveedor: " + rutWithDv);
        }
        return Long.valueOf(parts[0]);
    }

    public static String extractDv(String rutWithDv) {
        String[] parts = normalize(rutWithDv).split("-");
        if (parts.length < 2 || parts[1].isBlank()) {
            throw new ProcurementMappingException("Invalid rut proveedor: " + rutWithDv);
        }
        return parts[1];
    }

    private static String normalize(String rutWithDv) {
        return rutWithDv.trim()
                .replace(".", "")
                .replace(" ", "")
                .toUpperCase();
    }
}
