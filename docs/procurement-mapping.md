# Procurement CMP mapping

## Clasificacion de familia documental (Issue #28)

`ArtikosDocumentFamilyResolver` clasifica exclusivamente `DocumentoContable.tipoDocumento()`:
`33/34/56/61 -> CMP` y `885/886 -> HNR`. Rechaza documentos nulos, tipos ausentes
o desconocidos con `ProcurementMappingException`; no existe fallback a CMP ni a `tipoErp`.
Se eliminan espacios exteriores antes de consultar el catalogo.

Los fixtures `ZSVIDA_Nom15960*.xml`, `ZSVIDA_Nom15965_v2.xml` y
`ZSGRALES_Nom15961_v2.xml` contienen documentos con `Tipo_Documento` 33/34.
Los codigos numericos 56/61 estan contemplados por la normalizacion existente en
`ResultadoDocumento` y por `NominaResultXmlServiceTest`; aun no tienen fixtures XML propios.
El 801 de los fixtures corresponde a referencias, no al tipo del documento contable.
Los aliases ERP (`FEC`, `FCE`, `FC`, `OC`, etc.) no se agregan al catalogo de familia.

885/886 fueron confirmados en una nomina HNR real para #42. El Issue #32 sigue
abierto por definiciones funcionales y monetarias pendientes. No se derivan reglas monetarias ni
diferencias funcionales entre ambos. Desde #30, `ProcurementIntegrationService` usa
`ProcurementDocumentMapper.toDocumentRequest`, que resuelve la familia y delega CMP
al mapping existente. HNR falla con `ProcurementMappingException` y el mensaje
`Procurement HNR mapping is not implemented yet`, sin enviar un payload.
`ArtikosDocumentTypeMapper` (basado en `tipoErp`) y la propiedad
`procurement.mapping.document-type` conservan su uso interno CMP; la propiedad no decide el routing.

## Estado actual

Desde Sprint 9.5 el mapper soporta XML Artikos v2 y consulta homologaciones ASI para completar campos Procurement por linea de distribucion.

Fuentes principales:

- XML Artikos v2: `Tipo_ERP`, `Msg_To`, `USO_IVA`, `Quantity`, fechas, proveedor y distribuciones.
- ASI Oracle: `GRL_MAE_ITEM_DET` por `COD_CUENTA`, `COD_SISTEM` y `COD_IMPSTO`, mas validacion de maestro vigente en `GRL_MAE_ITEM`, para resolver `GRL_COD_ITEM`, `COD_TIP_UNID`, `COD_TIP_CNTA_ITEMS`, `COD_CONTBL`, `COD_SISTEM`, `NUM_PERIODO` y `COD_MONEDA`.
- Properties: constantes operativas como raiz `document-type`, filtro `cod-sistem`, tipo de cambio, descuentos e IVA.

Reglas nuevas:

- Raiz `COD_TIP_DOCUMT` sigue siendo `CMP`.
- `CMP_DOCUMT.COD_TIP_DOCUMT` se obtiene desde `Tipo_ERP`: `FEC/33`, `FCE/34`, `NDC/56`, `ECC/61`.
- `CMP_DOCUMT.COD_EMPRES` se resuelve desde `Msg_To`: `001/ZSGVIDA/ZSVIDA` -> `001`, `002/ZSGRALES` -> `002`; si falta, el mapper falla de forma controlada.
- `CMP_DOCUMT.CODIGO_REC_IVA` se obtiene desde `USO_IVA`; valores permitidos `U`, `R`, `N`; si viene vacio, default `U`.
- `CMP_DOCUMT.COD_MONEDA` se obtiene desde `GRL_MAE_ITEM_DET.COD_MONEDA`.
- `CMP_DOCUMT_DET.NUM_CANTDD` se obtiene desde `Conciliacion.Quantity`; si el tag no viene, se usa `1`.
- `CMP_DOCUMT_DET.GLS_LINEA` se arma con `Tipo_ERP + " " + Proveedor`.
- Cada distribucion consulta `GRL_MAE_ITEM_DET` por `COD_CUENTA`, `COD_SISTEM=CM` y `COD_IMPSTO`. Si `Monto_Neto > 0`, `COD_IMPSTO=IVA`; si `Monto_Neto = 0`, `COD_IMPSTO=EXE`. Luego valida el maestro vigente en `GRL_MAE_ITEM` usando `COD_EMPRES`, `NUM_PERIODO` y `GRL_COD_ITEM` devueltos por el detalle.
- Si las lineas devuelven distintos `COD_CONTBL`, el mapper falla con `ProcurementMappingException`.

## Alcance Sprint 9.0

Sprint 9.0 implementa el mapper Artikos -> Procurement CMP.

Sprint 9.1 agrega el cliente HTTP configurable para consumir Procurement, pero todavia no lo integra al flujo batch.

Endpoint objetivo futuro:

```http
POST /api/v1/document
```

Decisiones confirmadas:

- Solo se genera `CMP`.
- `HNR` queda fuera de alcance y se envia como `null`.
- No se implementa bulk.
- El alcance inicial no consultaba ASI; desde Sprint 9.5 el lookup ASI queda integrado para homologaciones de item.
- No se modifica el flujo SOAP Artikos.
- No se modifica `NOMFACTRES`.

## Estructura JSON

```json
{
  "COD_TIP_DOCUMT": "CMP",
  "CMP": {
    "CMP_DOCUMT": {},
    "CMP_DOCUMT_DET": [],
    "CMP_DOCUMT_DET_RUT": {}
  },
  "HNR": null
}
```

## Estrategia inicial

La estrategia inicial es uno a uno:

- un `DocumentoContable` Artikos genera un request Procurement CMP;
- cada `DistribucionContable` Artikos genera una linea `CMP_DOCUMT_DET`;
- `CMP_DOCUMT_DET_RUT` se completa con el RUT proveedor y vigencia activa para cumplir el contrato real de Procurement.

## Campo empresa

| Artikos `Msg_To` | COD_EMPRES |
| --- | --- |
| `001`, `ZSGVIDA`, `ZSVIDA` | `001` |
| `002`, `ZSGRALES` | `002` |

No existe fallback por profile para `COD_EMPRES`; si `Msg_To` falta o no esta soportado, el mapper detiene el envio a Procurement con error controlado.

## Campos constantes

| Procurement | Valor |
| --- | --- |
| Raiz `COD_TIP_DOCUMT` | `CMP` |
| `CMP_DOCUMT_DET_RUT.A_IND_VIGE` | `V` |
| `HNR` | `null` |

Aunque Artikos trae `Tipo_Documento` y `Tipo_ERP`, para esta integracion la raiz indica el tipo de payload `CMP`, mientras `CMP_DOCUMT.COD_TIP_DOCUMT` identifica el tipo interno `FEC`.

## Campos directos desde Artikos

| Artikos | Procurement | Regla |
| --- | --- | --- |
| `Documento.Rut_Proveedor` | `CMP_DOCUMT.NUM_RUT` | RUT sin digito verificador. Ejemplo: `96670840-9` -> `96670840`. |
| `Documento.Rut_Proveedor` | `CMP_DOCUMT_DET_RUT.CMP_NUM_RUT` | Se envia el mismo RUT proveedor sin digito verificador. |
| `Documento.Rut_Proveedor` | `CMP_DOCUMT_DET_RUT.NUM_RUT` | Se envia el mismo RUT proveedor sin digito verificador. |
| `Documento.Numero_Documento` | `CMP_DOCUMT.NUM_DOCCMP` | Valor directo. |
| `Documento.Fecha_Emision` | `CMP_DOCUMT.FEC_EMIDCM` | Formato de salida `yyyy-MM-dd`. |
| `Nomina.Fecha_Nomina` | `CMP_DOCUMT.FEC_COMPRB` | Formato de salida `yyyy-MM-dd`. |
| `Documento.Fecha_Vencimiento` | `CMP_DOCUMT.FEC_VNCCTA` | Si no viene, usar `Fecha_Emision`. |
| `Documento.Fecha_Recepcion` | `CMP_DOCUMT.FECHA_REC_FE` | Formato de salida `yyyy-MM-dd`. |
| `Documento.Monto_Neto` | `CMP_DOCUMT.MTO_TOT_NTODIG` | Valor directo, `0` si viene nulo. |
| `Documento.Monto_Exento` | `CMP_DOCUMT.MTO_TOT_EXNDIG` | Valor directo, `0` si viene nulo. |
| `Documento.Monto_IVA` | `CMP_DOCUMT.MTO_TOT_IVADIG` | Valor directo, `0` si viene nulo. |
| `Documento.Monto_Total` | `CMP_DOCUMT.MTO_TOT_DOCDIG` | Valor directo, `0` si viene nulo. |
| `Documento.Numero_Documento` | `CMP_DOCUMT.NUM_FOL_DOCUMT` | Valor numerico del folio/documento. |
| `Documento.Tipo_ERP` | `CMP_DOCUMT.COD_TIP_DOCUMT` | Se normaliza via `ArtikosDocumentTypeMapper`. Ejemplo `33` -> `FEC`; si ya viene `FEC`, se envia `FEC`. |
| `Documento.Tipo_ERP + Documento.Proveedor` | `CMP_DOCUMT.GLS_DOCUMT`, `CMP_DOCUMT_DET.GLS_LINEA` | Ejemplo `FEC DIMERC S.A.`. |
| `Conciliacion.Quantity` | `CMP_DOCUMT_DET.NUM_CANTDD` | Valor directo; si no viene, default `1`. |
| `Distribucion.Cod_CuentaContable` | `CMP_DOCUMT.COD_CUENTA` | Primera cuenta contable disponible en distribuciones. |
| `Distribucion.Cod_CuentaContable` | `CMP_DOCUMT_DET.COD_CUENTA` | Valor directo por linea; requerido por `ASI.CMP_DOCUMT_DET`. |
| `Distribucion.Cod_CentroCosto` | `CMP_DOCUMT_DET.COD_CCOSTO` | Valor directo. |
| `Distribucion.Monto_Neto` | `CMP_DOCUMT_DET.MTO_NETO` | Valor directo, `0` si viene nulo. |
| `Distribucion.Monto_Exento` | `CMP_DOCUMT_DET.MTO_EXENTO` | Valor directo, `0` si viene nulo. |
| `Distribucion.Monto_IVA` | `CMP_DOCUMT_DET.MTO_IVACLC` | Valor directo, `0` si viene nulo. |
| `Distribucion.Monto_Total` | `CMP_DOCUMT_DET.MTO_TOT_ITEM` | Valor directo, `0` si viene nulo. |

## Campos configurables por properties

| Property | Uso | Nota |
| --- | --- | --- |
| `procurement.mapping.document-type` | Raiz `COD_TIP_DOCUMT` | Default `CMP`. |
| `procurement.mapping.cod-sistem` | Filtro lookup ASI | Default `CM`. El valor enviado en `CMP_DOCUMT.COD_SISTEM` sale desde `GRL_MAE_ITEM_DET.COD_SISTEM`. |
| `procurement.mapping.val-tip-cambio` | `CMP_DOCUMT_DET.VAL_TIP_CAMBIO` | Default `1`. |
| `procurement.mapping.pct-dscnto` | `CMP_DOCUMT_DET.PCT_DSCNTO` | Default `0`. |
| `procurement.mapping.mto-dscnto` | `CMP_DOCUMT_DET.MTO_DSCNTO` | Default `0`. |
| `procurement.mapping.pct-iva` | `CMP_DOCUMT_DET.PCT_IVA` | Default `19`. |

Si falta una property obligatoria, el mapper lanza `ProcurementMappingException` con el nombre de la property.

## Campos resueltos desde ASI

Desde Sprint 9.5 se resuelven desde `GRL_MAE_ITEM_DET`:

- `COD_CONTBL`
- `COD_TIP_UNID`
- `GRL_COD_ITEM`
- `NUM_PERIODO`
- `COD_TIP_CNTA_ITEMS`
- `COD_SISTEM`
- `COD_MONEDA`

La validacion de cuenta contable, centro de costo y periodo abierto sigue quedando del lado de Procurement/ASI.

## Pendientes

- Validar catalogos ASI para evitar FK de cuenta o centro de costo al insertar en Procurement.
- Definir idempotencia para documentos enviados a Procurement.
- Definir estrategia de reintentos y errores al consumir `POST /api/v1/document`.
- Evaluar endpoint bulk en sprint posterior.

## HTTP client

El cliente HTTP Procurement vive en `ProcurementClient` y usa `RestClient`.

Properties:

```properties
procurement.client.enabled=false
procurement.client.base-url=
procurement.client.document-path=/api/v1/document
procurement.client.connect-timeout-ms=5000
procurement.client.read-timeout-ms=30000
```

Reglas iniciales:

- `enabled=false`: el cliente falla de forma controlada al intentar usarlo.
- HTTP `2xx` con `statusCode=0`: resultado exitoso.
- HTTP `2xx` con `statusCode!=0`: NOK funcional.
- HTTP `4xx` con body parseable: NOK funcional.
- HTTP `5xx`: error tecnico.
- Timeout/conexion: error tecnico.

El cliente no loguea el JSON completo en `INFO`. Request y response completos quedan reservados para `DEBUG`.

## Preparacion HNR (Issue #42)

Para los tipos 885 y 886 se confirma funcionalmente el lookup `HO` + `RET`,
incluso si la boleta no tiene retencion monetaria. Configuracion independiente:

```properties
procurement.mapping.hnr.cod-sistem=HO
procurement.mapping.hnr.cod-impsto=RET
procurement.mapping.hnr.pct-retn=12.5
```

Overrides por entorno: `PROCUREMENT_MAPPING_HNR_COD_SISTEM`,
`PROCUREMENT_MAPPING_HNR_COD_IMPSTO` y `PROCUREMENT_MAPPING_HNR_PCT_RETN`.
El porcentaje 12.5 es provisional de desarrollo, entregado por el propietario;
no es una tasa tributaria definitiva validada para produccion y no se utiliza
para calcular importes ni para resolver el lookup.

`ProcurementDistributionResolver` clasifica la familia con
`ArtikosDocumentFamilyResolver` y conserva un unico recorrido y un unico
`ProcurementMappingLookupService` sobre `GRL_MAE_ITEM_DET`. HNR toma sistema e
impuesto de su configuracion; CMP conserva su sistema existente y
`ProcurementTaxTypeResolver` (IVA/EXE). `ResolvedDistribution` no cambia.

La integracion traspasa datos: no es un motor contable ni tributario. No calcula,
recalcula, distribuye ni modifica importes originales sin aprobacion funcional
explicita. No se implementan porcentajes efectivos, prorrateos, montos liquidos
ni transformaciones de Quantity. El fixture sintetico `hnr-synthetic-v2.xml`
preserva SOAP Artikos v2.0, Tipo_ERP vacio, BH/ADJ, Quantity=0 y los importes
recibidos: 885 (8000/0/0/8000) y 886 (294985/44985/0/294985), en orden
Neto/IVA/Exento/Total. Las distribuciones conservan bruto y Monto_IVA=0.
No incluye PDF originales, identidades reales ni tokens.

El Issue #32 permanece abierto: falta aprobar el mapeo financiero definitivo,
el tratamiento de retenciones y montos liquidos y la tasa definitiva para
produccion. No se avanza a #33 ni se construyen payloads HNR.
El routing productivo conserva `Procurement HNR mapping is not implemented yet`
antes de consultar ASI o enviar documentos desde `ProcurementIntegrationService`.
