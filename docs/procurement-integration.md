# Procurement integration

## Alcance actual

La aplicacion ya cuenta con:

- mapper Artikos -> Procurement CMP;
- DTOs JSON para `CMP`;
- cliente HTTP configurable para `POST /api/v1/document`;
- integracion opcional al processor real del batch;
- lookup ASI operativo contra `GRL_MAE_ITEM` y `GRL_MAE_ITEM_DET`;
- idempotencia funcional para `statusCode=-20`;
- flujo local XML validado para pruebas end-to-end controladas.

La integracion con el flujo batch queda controlada por feature flag. Por defecto esta deshabilitada para mantener el comportamiento operacional existente.

## Estado actual

- Mapper Procurement `CMP` operativo.
- Lookup ASI operativo para item, unidad, tipo cuenta, contable, sistema, periodo y moneda.
- Cliente HTTP Procurement operativo.
- Integracion batch operativa bajo `procurement.integration.enabled`.
- `statusCode=0` tratado como OK.
- `statusCode=-20` con mensaje de duplicado tratado como OK idempotente.
- Flujo local XML validado.
- Pendiente validacion con nomina real Artikos QA en estado correcto para `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`.

El cierre funcional y las evidencias sanitizadas estan documentadas en:

- `docs/procurement-functional-closure.md`
- `docs/asi-lookup.md`
- `docs/evidence/procurement-local-e2e.md`

## Endpoint objetivo

```http
POST /api/v1/document
```

## Properties

Configuracion base:

```properties
procurement.client.enabled=false
procurement.client.base-url=
procurement.client.document-path=/api/v1/document
procurement.client.connect-timeout-ms=5000
procurement.client.read-timeout-ms=30000
procurement.integration.enabled=false
```

QA/PROD:

```properties
procurement.client.enabled=true
procurement.client.base-url=${PROCUREMENT_BASE_URL}
procurement.client.document-path=/api/v1/document
procurement.client.connect-timeout-ms=${PROCUREMENT_CONNECT_TIMEOUT_MS:5000}
procurement.client.read-timeout-ms=${PROCUREMENT_READ_TIMEOUT_MS:30000}
procurement.integration.enabled=${PROCUREMENT_INTEGRATION_ENABLED:false}
```

Si `procurement.client.enabled=false`, el cliente no llama Procurement y lanza una excepcion controlada al intentar usarse.

`procurement.integration.enabled` controla si el processor batch usa Procurement:

- `false`: usa procesamiento local/simulado existente.
- `true`: por cada documento Artikos genera request CMP y llama `POST /api/v1/document`.

Excepcion operacional: si el job se inicia con `dryRun=true`, el batch fuerza procesamiento simulado y no llama Procurement, aunque `procurement.integration.enabled=true`.

Para activar la ruta real se requiere que ambos flags esten activos:

```properties
procurement.client.enabled=true
procurement.integration.enabled=true
```

## Request

El request se construye desde `ProcurementDocumentMapper`.

Estructura:

```json
{
  "COD_TIP_DOCUMT": "CMP",
  "CMP": {
    "CMP_DOCUMT": {},
    "CMP_DOCUMT_DET": [],
    "CMP_DOCUMT_DET_RUT": {
      "CMP_NUM_RUT": 96670840,
      "NUM_RUT": 96670840,
      "A_IND_VIGE": "V"
    }
  },
  "HNR": null
}
```

## Response esperado

Contrato base esperado:

```json
{
  "payload": {},
  "statusCode": 0,
  "message": "OK",
  "error": null
}
```

Reglas:

- `statusCode=0`: OK funcional.
- `statusCode=-20`: documento ya existente, OK idempotente si corresponde a duplicado conocido.
- `statusCode!=0`: NOK funcional si HTTP fue valido o el body es parseable.
- `payload.externalDocumentId`, `payload.documentId` o `payload.id` puede usarse como identificador externo si Procurement lo entrega.

## Flujo batch con Procurement

Cuando `procurement.integration.enabled=true`:

1. `ArtikosNominaItemProcessor` confirma la nomina con `NOMFACTCONFIR`.
2. `NominaProcessingService` delega el procesamiento documental a `ProcurementDocumentProcessingService`.
3. Por cada `DocumentoContable`, `ProcurementIntegrationService` mapea Artikos -> CMP y llama `ProcurementClient.postDocument`.
4. `ProcurementResultMapper` convierte la respuesta Procurement en `ResultadoDocumento`.
5. `NominaResultXmlService` genera `NOMFACTRES` con el estado final por documento.
6. `ArtikosNominaResultItemWriter` envia `NOMFACTRES` y actualiza `CONTROL_NOMINA`.

La unidad principal del batch sigue siendo la nomina. Procurement se invoca documento a documento dentro del procesamiento de esa nomina.

Si `NOMFACTCONFIR` es rechazado, el processor marca `CONTROL_NOMINA` como `ERROR`, falla el job y no ejecuta Procurement para esa nomina.

En modo `artikos.source.mode=local-xml`, el batch toma la nomina desde XML local, omite llamadas reales a `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`, pero puede llamar Procurement si `procurement.integration.enabled=true` y `procurement.client.enabled=true`. Ver `docs/local-e2e-testing.md`.

## Politica funcional

- Documento aceptado por Procurement: `statusCode=0`, `ResultadoDocumento.status=OK`.
- Documento ya existente en Procurement/ASI: se interpreta como OK idempotente y cuenta como `ResultadoDocumento.status=OK`.
- Documento rechazado funcionalmente por Procurement: `statusCode!=0`, `ResultadoDocumento.status=NOK`.
- Una nomina con uno o mas documentos `NOK` no falla el job por esa razon; se informa a Artikos via `NOMFACTRES` y `CONTROL_NOMINA` queda `NOK` si el envio de resultado fue exitoso.

## Idempotencia

No se crea una tabla adicional de detalle por documento. La unica tabla funcional propia del adapter sigue siendo `CONTROL_NOMINA`.

La idempotencia se divide en dos niveles:

- `CONTROL_NOMINA` controla reproceso por nomina.
- Procurement/ASI controla duplicidad por documento.

Antes de confirmar una nomina y antes de llamar Procurement, el processor consulta el ultimo registro `CONTROL_NOMINA` disponible por `NUMERO_NOMINA`.

Reglas actuales:

- Ultimo estado `OK`: no se vuelve a enviar documentos a Procurement. Se genera un resultado OK controlado por documento para permitir cerrar el flujo Artikos con `NOMFACTRES`.
- Ultimo estado `NOK`: se permite reproceso.
- Ultimo estado `ERROR`: se permite reproceso.
- Ultimo estado `PROCESSING`: se permite reproceso controlado por ahora y se registra log.
- Sin registro previo: se procesa normalmente.

Limitacion actual: `CONTROL_NOMINA` no tiene columna de empresa, `profile` o `COD_EMPRES`, por lo que la busqueda de reproceso se hace solo por `NUMERO_NOMINA`.

Si Procurement responde que el registro ya existe, el adapter lo trata como OK idempotente. Mensajes conocidos:

- `El registro que intenta crear ya existe en la base de datos`
- `registro ya existe`
- `ya existe`
- `duplicate`
- `duplicado`
- `unique constraint`
- `ORA-00001`

El mensaje de documento informado para ese caso es:

```text
Documento ya existia en Procurement/ASI
```

Errores funcionales distintos a duplicado siguen contando como `NOK`. Errores tecnicos siguen fallando la nomina/job.

### Respuesta idempotente confirmada

Procurement confirmo esta respuesta real cuando el documento ya existe:

```json
{
  "payload": null,
  "statusCode": -20,
  "message": null,
  "error": "El registro que intenta crear ya existe en la base de datos"
}
```

El adapter interpreta `statusCode=-20` como documento ya existente y lo transforma en `ResultadoDocumento.status=OK` con mensaje idempotente. Esto permite informar OK a Artikos en `NOMFACTRES` y evita que un reproceso falle por duplicados ya controlados por Procurement/ASI.

A futuro, lo ideal es que Procurement documente formalmente `-20` como un codigo funcional `DOCUMENT_ALREADY_EXISTS`.

## Politica tecnica

Si ocurre un error tecnico Procurement, se detiene la nomina:

- timeout;
- error de conexion;
- HTTP `5xx`;
- request no serializable;
- response no parseable;
- error de mapeo Artikos -> CMP.

Estos casos se traducen a `ArtikosIntegrationException` con:

- `PROCUREMENT_TECHNICAL_ERROR` para fallas HTTP/red/serializacion/parseo;
- `PROCUREMENT_MAPPING_ERROR` para fallas de mapeo o configuracion requerida.

El processor marca `CONTROL_NOMINA` como `ERROR`, el job termina `FAILED` y no se debe enviar `NOMFACTRES` para esa nomina.

## Errores tecnicos

Se consideran errores tecnicos y lanzan `ProcurementClientException`:

- timeout;
- error de conexion;
- HTTP `5xx`;
- error de serializacion;
- respuesta no parseable.

HTTP `4xx` con body parseable se trata como NOK funcional inicial, no como excepcion tecnica.

## Logging

En `INFO` se registra:

- inicio de llamada;
- endpoint sanitizado;
- HTTP status;
- `statusCode` funcional;
- duracion.

No se loguea el JSON completo en `INFO`. Request y response completos quedan reservados para `DEBUG`.

## Fuera de alcance

- Implementar bulk.
- Implementar retry Procurement.
- Modificar Artikos SOAP.
- Cambiar `NOMFACTRES`.
- Crear tabla adicional de auditoria por documento.
- Definir contrato formal de duplicado en Procurement.
