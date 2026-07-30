package cl.atk.nomina.batch.domain;

import java.math.BigDecimal;

public record ResultadoDocumento(
        SimulatedDocumentoContable simulatedDocumento,
        String status,
        String message,
        String docFolio,
        String docRutProveedor,
        String docTipoDoc,
        BigDecimal monto) {

    public ResultadoDocumento(
            SimulatedDocumentoContable simulatedDocumento,
            String status,
            String message) {
        this(simulatedDocumento, status, message, null, null, null, null);
    }

    public boolean isOk() {
        return "OK".equals(status);
    }

    public String resolvedDocFolio() {
        if (hasText(docFolio)) {
            return docFolio;
        }
        if (originalDocumento() == null || originalDocumento().idDocumento() == null) {
            return "";
        }
        return originalDocumento().idDocumento().toString();
    }

    public String resolvedDocRutProveedor() {
        if (hasText(docRutProveedor)) {
            return docRutProveedor;
        }
        return originalDocumento() == null ? "" : originalDocumento().rutProveedor();
    }

    public String resolvedDocTipoDoc() {
        if (hasText(docTipoDoc)) {
            return normalizeDocTipoDoc(docTipoDoc);
        }
        if (originalDocumento() == null) {
            return "";
        }
        if (hasText(originalDocumento().tipoErp())) {
            return normalizeDocTipoDoc(originalDocumento().tipoErp());
        }
        return normalizeDocTipoDoc(originalDocumento().tipoDocumento());
    }

    public BigDecimal resolvedMonto() {
        if (monto != null) {
            return monto;
        }
        return originalDocumento() == null ? null : originalDocumento().montoTotal();
    }

    public DocumentoContable originalDocumento() {
        return simulatedDocumento == null ? null : simulatedDocumento.documentoOriginal();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeDocTipoDoc(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.trim()) {
            case "33" -> "FEC";
            case "34" -> "FCE";
            case "56" -> "NDC";
            case "61" -> "ECC";
            default -> value.trim();
        };
    }
}
