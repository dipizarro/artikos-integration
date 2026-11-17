package cl.atk.nomina.batch.procurement.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcurementDocumentRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesHnrWithExactReferenceContract() throws Exception {
        ProcurementDocumentRequest request = new ProcurementDocumentRequest(
                "HNR",
                null,
                new ProcurementHnrRequest(
                        new ProcurementHnrDocumtRequest(
                                "HNR", "01", 2026, 100000008L, 900001L,
                                "PROC", "1", "01", 2026, 4101001L, "2",
                                "2026-06-09", "Documento HNR prueba local",
                                new BigDecimal("100000"), new BigDecimal("10000"), new BigDecimal("90000"),
                                "V", "2026-06-09", "2026-07-09"),
                        List.of(new ProcurementHnrDocumtDetRequest(
                                1, "01", 2026, "SERV001", "PROC", "01", 2026,
                                1001L, "01", 2026, 4101001L, "2", "1", "CLP", "UN",
                                "2026-06-09", "Linea de prueba HNR", BigDecimal.ONE,
                                new BigDecimal("100000"), new BigDecimal("100000"), BigDecimal.ONE,
                                BigDecimal.ZERO, new BigDecimal("100000"), new BigDecimal("10"),
                                new BigDecimal("10000"), "V")),
                        new ProcurementHnrDocumtDetRutRequest(100000008L, 100000008L, "V")));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));
        JsonNode expected = objectMapper.readTree("""
                {
                  "COD_TIP_DOCUMT": "HNR",
                  "CMP": null,
                  "HNR": {
                    "HNR_DOCUMT": {
                      "COD_TIP_DOCUMT": "HNR",
                      "COD_EMPRES": "01",
                      "NUM_PERIODO": 2026,
                      "NUM_RUT": 100000008,
                      "NUM_DOCHON": 900001,
                      "COD_SISTEM": "PROC",
                      "COD_CONTBL": "1",
                      "CON_COD_EMPRES": "01",
                      "CON_NUM_PERIODO": 2026,
                      "COD_CUENTA": 4101001,
                      "COD_TIP_CUENTA": "2",
                      "FEC_EMIDCM": "2026-06-09",
                      "GLS_DOCUMT": "Documento HNR prueba local",
                      "MTO_TOTL_HNR": 100000,
                      "MTO_RETN_HNR": 10000,
                      "MTO_NETO_HNR": 90000,
                      "A_IND_VIGE": "V",
                      "FEC_COMPRB": "2026-06-09",
                      "FEC_VNCCTA": "2026-07-09"
                    },
                    "HNR_DOCUMT_DET": [
                      {
                        "NUM_LIN_DOCHON": 1,
                        "GRL_COD_EMPRES": "01",
                        "GRL_NUM_PERIODO": 2026,
                        "GRL_COD_ITEM": "SERV001",
                        "COD_SISTEM": "PROC",
                        "GRL2_COD_EMPRES": "01",
                        "GRL2_NUM_PERIODO": 2026,
                        "COD_CCOSTO": 1001,
                        "CON_COD_EMPRES": "01",
                        "CON_NUM_PERIODO": 2026,
                        "COD_CUENTA": 4101001,
                        "COD_TIP_CUENTA": "2",
                        "COD_CONTBL": "1",
                        "COD_MONEDA": "CLP",
                        "COD_TIP_UNID": "UN",
                        "FEC_EMIDCM": "2026-06-09",
                        "GLS_LINEA": "Linea de prueba HNR",
                        "NUM_CANTDD": 1,
                        "VAL_UNIT_MNDORG": 100000,
                        "MTO_MNDORG": 100000,
                        "VAL_TIP_CAMBIO": 1,
                        "PCT_DSCNTO": 0,
                        "MTO_TOT_ITEM": 100000,
                        "PCT_RETN": 10,
                        "MTO_RETN_ITEM": 10000,
                        "A_IND_VIGE": "V"
                      }
                    ],
                    "HNR_DOCUMT_DET_RUT": {
                      "HNR_NUM_RUT": 100000008,
                      "NUM_RUT": 100000008,
                      "A_IND_VIGE": "V"
                    }
                  }
                }
                """);

        assertThat(json).isEqualTo(expected);
    }

    @Test
    void serializesWithExactProcurementJsonNames() throws Exception {
        ProcurementDocumentRequest request = new ProcurementDocumentRequest(
                "CMP",
                new ProcurementCmpRequest(
                        new ProcurementCmpDocumtRequest(
                                "FEC",
                                "002",
                                202606,
                                96670840L,
                                "2",
                                "CM",
                                "21040010",
                                "2",
                                "CONTBL",
                                "CLP",
                                "2026-06-03",
                                "Documento",
                                "2026-06-03",
                                new BigDecimal("19000"),
                                BigDecimal.ZERO,
                                new BigDecimal("2850"),
                                new BigDecimal("21850"),
                                2L,
                                "2026-06-03",
                                "REC",
                                "2026-06-03"),
                        List.of(new ProcurementCmpDocumtDetRequest(
                                1,
                                "UN",
                                "SERVICIO",
                                "13102",
                                "21040010",
                                "2",
                                "BENEFICIOS AL PERSONAL",
                                BigDecimal.ONE,
                                new BigDecimal("15000"),
                                new BigDecimal("15000"),
                                BigDecimal.ONE,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                new BigDecimal("15000"),
                                new BigDecimal("19"),
                                new BigDecimal("2090"),
                                new BigDecimal("17090"))),
                        new ProcurementCmpDocumtDetRutRequest(96670840L, 96670840L, "V")),
                null);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(json.get("COD_TIP_DOCUMT").asText()).isEqualTo("CMP");
        assertThat(json.has("CMP")).isTrue();
        assertThat(json.has("HNR")).isTrue();
        assertThat(json.get("HNR").isNull()).isTrue();
        assertThat(json.get("CMP").has("CMP_DOCUMT")).isTrue();
        assertThat(json.get("CMP").has("CMP_DOCUMT_DET")).isTrue();
        assertThat(json.get("CMP").has("CMP_DOCUMT_DET_RUT")).isTrue();
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("COD_EMPRES").asText()).isEqualTo("002");
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("COD_TIP_DOCUMT").asText()).isEqualTo("FEC");
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("NUM_DOCCMP").asText()).isEqualTo("2");
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("FEC_EMIDCM").asText()).isEqualTo("2026-06-03");
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("FEC_COMPRB").asText()).isEqualTo("2026-06-03");
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("COD_TIP_CUENTA").asText()).isEqualTo("2");
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("MTO_TOT_DOCDIG").decimalValue())
                .isEqualByComparingTo("21850");
        assertThat(json.get("CMP").get("CMP_DOCUMT").get("NUM_FOL_DOCUMT").asLong()).isEqualTo(2L);
        assertThat(json.get("CMP").get("CMP_DOCUMT_DET").get(0).get("NUM_LIN_DOCCMP").asInt()).isEqualTo(1);
        assertThat(json.get("CMP").get("CMP_DOCUMT_DET").get(0).get("COD_CUENTA").asText()).isEqualTo("21040010");
        assertThat(json.get("CMP").get("CMP_DOCUMT_DET").get(0).get("COD_TIP_CUENTA").asText()).isEqualTo("2");
        assertThat(json.get("CMP").get("CMP_DOCUMT_DET").get(0).get("GLS_LINEA").asText())
                .isEqualTo("BENEFICIOS AL PERSONAL");
        assertThat(json.get("CMP").get("CMP_DOCUMT_DET_RUT").get("CMP_NUM_RUT").asLong()).isEqualTo(96670840L);
        assertThat(json.get("CMP").get("CMP_DOCUMT_DET_RUT").get("NUM_RUT").asLong()).isEqualTo(96670840L);
        assertThat(json.get("CMP").get("CMP_DOCUMT_DET_RUT").get("A_IND_VIGE").asText()).isEqualTo("V");
    }
}
