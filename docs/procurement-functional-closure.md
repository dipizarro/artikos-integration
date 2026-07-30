# Procurement functional closure

## Objetivo

Documentar el cierre funcional de la integracion Procurement del servicio `atk-nomina-batch` y dejar una base de evidencia sanitizada para la validacion QA real con Artikos.

La integracion permite tomar una nomina Artikos, mapear cada documento a un request Procurement `CMP`, enviar cada documento a `POST /api/v1/document`, interpretar la respuesta funcional y generar el resultado Artikos `NOMFACTRES`.

## Alcance implementado

- Modo `local-xml` para pruebas end-to-end controladas sin depender de nominas disponibles en Artikos QA.
- Parser Artikos XML v2 hacia modelo interno.
- Mapper Artikos -> Procurement `CMP`.
- Lookup ASI contra `GRL_MAE_ITEM` y `GRL_MAE_ITEM_DET`.
- Cliente HTTP Procurement para `POST /api/v1/document`.
- Integracion Procurement en el processor real del batch bajo `procurement.integration.enabled`.
- Interpretacion de `statusCode=0` como OK.
- Interpretacion de `statusCode=-20` con mensaje de duplicado como OK idempotente.
- Interpretacion de errores funcionales Procurement como documento NOK.
- Interpretacion de errores tecnicos Procurement como nomina ERROR y job FAILED.
- Generacion de `NOMFACTRES`.
- Actualizacion de `CONTROL_NOMINA`.

## Flujo funcional final

```text
POST /api/v1/nominas/batch/start
  -> Reader obtiene nomina Artikos
  -> Parser convierte XML a modelo interno
  -> Processor confirma recepcion si aplica
  -> Processor procesa documentos
  -> Mapper genera JSON Procurement CMP
  -> Lookup ASI completa GRL_COD_ITEM, COD_TIP_UNID, COD_CONTBL, COD_TIP_CUENTA, COD_SISTEM, NUM_PERIODO y COD_MONEDA
  -> ProcurementClient llama POST /api/v1/document
  -> ResultadoDocumento OK/NOK
  -> Writer genera NOMFACTRES
  -> Writer envia o simula envio segun configuracion
  -> CONTROL_NOMINA queda OK/NOK/ERROR
```

## Flujo tecnico final

1. `NominaBatchStartController` inicia el job `nominaDocumentosContablesJob`.
2. `ArtikosNominaItemReader` obtiene una nomina desde Artikos remoto o desde XML local segun `artikos.source.mode`.
3. `NominaXmlParserService` parsea el SOAP/XML y construye `Nomina`.
4. `ArtikosNominaItemProcessor` confirma recepcion con `NOMFACTCONFIR` cuando corresponde.
5. `NominaProcessingService` procesa la nomina y delega el procesamiento documental a Procurement si `procurement.integration.enabled=true`.
6. `ProcurementDocumentMapper` construye el request `CMP`.
7. `ProcurementMappingLookupService` consulta ASI para homologaciones de item.
8. `ProcurementClient` invoca `POST /api/v1/document`.
9. `ProcurementResultMapper` transforma la respuesta en `ResultadoDocumento`.
10. `ArtikosNominaResultItemWriter` genera `NOMFACTRES`, lo envia si corresponde y actualiza `CONTROL_NOMINA`.

## Reglas principales de mapeo

| Campo Procurement | Origen | Regla |
| --- | --- | --- |
| Raiz `COD_TIP_DOCUMT` | Property | `procurement.mapping.document-type`, default `CMP`. |
| `CMP_DOCUMT.COD_TIP_DOCUMT` | `Tipo_ERP` XML | `FEC/FCE/NDC/ECC`; tambien acepta codigos `33/34/56/61` y los homologa. |
| `CMP_DOCUMT.COD_EMPRES` | `Msg_To` XML | `001`, `ZSGVIDA` o `ZSVIDA` -> `001`; `002` o `ZSGRALES` -> `002`. Si falta, error controlado. |
| `CMP_DOCUMT.COD_SISTEM` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.COD_SISTEM`. La property `procurement.mapping.cod-sistem` solo filtra el lookup. |
| `CMP_DOCUMT.NUM_PERIODO` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.NUM_PERIODO`. |
| `CMP_DOCUMT.COD_TIP_CUENTA` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.COD_TIP_CNTA_ITEMS`. |
| `CMP_DOCUMT.COD_CONTBL` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.COD_CONTBL`; si hay mas de un valor por nomina/documento, el mapper falla. |
| `CMP_DOCUMT.COD_MONEDA` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.COD_MONEDA`. |
| `CMP_DOCUMT.CODIGO_REC_IVA` | `USO_IVA` XML | Valores `U/R/N`; vacio o nulo = `U`. |
| `CMP_DOCUMT.GLS_DOCUMT` | `Tipo_ERP` + `Proveedor` XML | Ejemplo `FEC PROVEEDOR DEMO S.A.`. |
| `CMP_DOCUMT_DET.GRL_COD_ITEM` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.GRL_COD_ITEM`. |
| `CMP_DOCUMT_DET.COD_TIP_UNID` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.COD_TIP_UNID`. |
| `CMP_DOCUMT_DET.COD_TIP_CUENTA` | Lookup ASI | Sale desde `GRL_MAE_ITEM_DET.COD_TIP_CNTA_ITEMS`. |
| `CMP_DOCUMT_DET.COD_CCOSTO` | Distribucion Artikos | Valor directo desde la distribucion. |
| `CMP_DOCUMT_DET.COD_CUENTA` | `Cod_CuentaContable` XML | Valor directo desde la distribucion. |
| `CMP_DOCUMT_DET.GLS_LINEA` | `Tipo_ERP` + `Proveedor` XML | Misma regla de glosa documental. |
| `CMP_DOCUMT_DET.NUM_CANTDD` | `Quantity` XML | Valor de la conciliacion; si no viene, default `1`. |
| `CMP_DOCUMT_DET.VAL_TIP_CAMBIO` | Property | `procurement.mapping.val-tip-cambio`. |
| `CMP_DOCUMT_DET.PCT_DSCNTO` | Property | `procurement.mapping.pct-dscnto`. |
| `CMP_DOCUMT_DET.MTO_DSCNTO` | Property | `procurement.mapping.mto-dscnto`. |
| `CMP_DOCUMT_DET.PCT_IVA` | Property | `procurement.mapping.pct-iva`. |

## Reglas de respuesta Procurement

| statusCode / condicion | Interpretacion adapter | Resultado Artikos |
| --- | --- | --- |
| `0` | OK normal. | `DocEstado=OK`. |
| `-20` con error `El registro que intenta crear ya existe en la base de datos` | OK idempotente. | `DocEstado=OK`, `DocDescEstado=Documento ya existia en Procurement/ASI`. |
| Otro `statusCode` funcional | NOK funcional. | `DocEstado=NOK`, descripcion con codigo y mensaje Procurement. |
| HTTP timeout, conexion, 5xx, serializacion o respuesta no parseable | ERROR tecnico. | `CONTROL_NOMINA=ERROR`, job `FAILED`, no se debe enviar `NOMFACTRES`. |
| Error de mapeo o lookup ASI | ERROR tecnico/controlado. | `CONTROL_NOMINA=ERROR`, job `FAILED`. |

## Reglas de idempotencia

- `CONTROL_NOMINA` controla reproceso por nomina.
- Si la ultima nomina esta `OK`, el adapter evita reprocesar documentos y genera resultado OK controlado.
- Si la ultima nomina esta `NOK` o `ERROR`, el adapter permite reproceso.
- Si Procurement informa duplicado conocido con `statusCode=-20`, el documento se considera OK idempotente.
- No existe tabla propia de detalle por documento en este alcance.

## Reglas de error

- Rechazo `NOMFACTCONFIR`: `CONTROL_NOMINA=ERROR`, job `FAILED`, no llama Procurement.
- Error funcional Procurement por documento: documento `NOK`; la nomina puede terminar `NOK` y el job `COMPLETED` si se logra cerrar con `NOMFACTRES`.
- Error tecnico Procurement: `CONTROL_NOMINA=ERROR`, job `FAILED`.
- Error lookup ASI: `CONTROL_NOMINA=ERROR`, job `FAILED`.
- Rechazo o error `NOMFACTRES`: `CONTROL_NOMINA=ERROR`, job `FAILED`.

## Validado

- XML local Artikos v2 parseado correctamente.
- Mapper `CMP` genera estructura JSON esperada.
- Lookup ASI obtiene item, unidad, tipo cuenta, contable, sistema, periodo y moneda.
- Cliente HTTP Procurement invoca `POST /api/v1/document`.
- `statusCode=0` se interpreta como OK.
- `statusCode=-20` de duplicado se interpreta como OK idempotente.
- Errores tecnicos Procurement fallan el job y se registran en `CONTROL_NOMINA`.
- `NOMFACTRES` se genera con documentos OK/NOK.
- Flujo local XML ejecuta sin llamadas reales a Artikos remoto cuando `artikos.source.mode=local-xml`.

## Pendiente para QA real Artikos

- Validar `NOMFACTERP` real con nominas disponibles.
- Validar `NOMFACTCONFIR` real con nominas en estado correcto para confirmacion.
- Validar `NOMFACTRES` real contra Artikos QA.
- Validar catalogo formal de errores Procurement.
- Validar regla de `COD_IMPSTO` con negocio.
- Validar mapeo completo con usuarios funcionales ASI/Procurement.
- Confirmar si se requiere bulk por performance.

## Replay local recomendado antes de remoto real

Antes de ejecutar el flujo remoto completo Artikos + Adapter + Procurement, se recomienda capturar el XML real con SoapUI y ejecutarlo en modo `artikos.source.mode=local-xml`.

Este paso permite validar parser, mapper Procurement, lookup ASI, respuesta Procurement, `NOMFACTRES` local y `CONTROL_NOMINA` sin confirmar ni cerrar la nomina en Artikos.

Procedimiento: `docs/artikos-replay-local.md`.

## Siguiente hito: validacion remota real

El flujo local XML fue validado como paso previo. El siguiente hito es la primera ejecucion remota real con `NOMFACTERP`, `NOMFACTCONFIR`, Procurement y `NOMFACTRES` reales.

Procedimiento: `docs/artikos-remote-e2e.md`.

## Fuera de alcance

- Implementar bulk.
- Implementar seguridad propia en la aplicacion.
- Cambiar DDL.
- Crear auditoria documental propia.
- Modificar Procurement.
- Cambiar contratos SOAP Artikos.
- Versionar datos sensibles reales.
