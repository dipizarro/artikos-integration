package cl.atk.nomina.batch.batch.processor;

import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import java.math.BigDecimal;
import org.springframework.batch.item.ItemProcessor;

public class NominaDocumentoItemProcessor implements ItemProcessor<SimulatedDocumentoContable, ResultadoDocumento> {

    @Override
    public ResultadoDocumento process(SimulatedDocumentoContable item) {
        String validationMessage = validate(item);
        boolean ok = validationMessage.isBlank();
        return new ResultadoDocumento(
                item,
                ok ? "OK" : "NOK",
                ok ? "Documento validado correctamente" : validationMessage);
    }

    private String validate(SimulatedDocumentoContable item) {
        if (item.documentoOriginal().montoTotal() == null
                || item.documentoOriginal().montoTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return "Monto_Total debe ser mayor a cero";
        }
        if (isBlank(item.documentoOriginal().rutProveedor())) {
            return "Rut_Proveedor es requerido";
        }
        if (isBlank(item.documentoOriginal().tipoDocumento())) {
            return "Tipo_Documento es requerido";
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
