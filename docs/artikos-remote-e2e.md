# Artikos remote end-to-end execution

## Objetivo

Preparar la primera ejecucion remota real Artikos + Adapter + Procurement usando el flujo completo:

```text
Artikos NOMFACTERP real
  -> Adapter
  -> Artikos NOMFACTCONFIR real
  -> Procurement POST /api/v1/document real
  -> Artikos NOMFACTRES real
  -> CONTROL_NOMINA
```

Esta guia es operacional. No agrega endpoints, no cambia DDL y no modifica Procurement.

## Precondiciones

- Artikos confirma que existe una nomina disponible para el profile que se ejecutara.
- El XML de esa nomina fue validado previamente con replay local.
- Procurement esta disponible.
- Oracle ASI esta disponible.
- Tabla `CONTROL_NOMINA` creada.
- Metadata Spring Batch `BATCH_*` creada.
- `GRL_MAE_ITEM` y `GRL_MAE_ITEM_DET` tienen datos vigentes para las cuentas de la nomina.
- Properties Artikos configuradas para el profile.
- Properties Procurement configuradas.
- El equipo conoce el resultado esperado: OK, NOK funcional o duplicado idempotente.

## Configuracion requerida

Para esta prueba, la aplicacion debe ejecutar en modo remoto:

```properties
artikos.source.mode=remote
artikos.confirm.enabled=true
artikos.result.enabled=true

procurement.integration.enabled=true
procurement.client.enabled=true

app.diagnostics.enabled=false
app.admin.enabled=false
app.endpoints.operations.enabled=false
```

Significado:

- `artikos.source.mode=remote`: consume `NOMFACTERP` real.
- `artikos.confirm.enabled=true`: envia `NOMFACTCONFIR` real.
- `artikos.result.enabled=true`: envia `NOMFACTRES` real.
- `procurement.integration.enabled=true`: procesa documentos contra Procurement.
- `procurement.client.enabled=true`: habilita el cliente HTTP Procurement.
- `app.diagnostics.enabled=false`: no expone `/api/v1/dev/**`.
- `app.admin.enabled=false`: no expone endpoints administrativos.
- `app.endpoints.operations.enabled=false`: deja solo `POST /api/v1/nominas/batch/start` como contrato productivo inicial.

No ejecutar esta prueba con `artikos.source.mode=local-xml`.

## Checklist antes de ejecutar

- [ ] Artikos confirma que existe nomina disponible.
- [ ] Se valido previamente XML de esa nomina mediante replay local.
- [ ] Procurement esta disponible.
- [ ] Oracle ASI esta disponible.
- [ ] `CONTROL_NOMINA` esta creada.
- [ ] Metadata Spring Batch esta creada.
- [ ] `GRL_MAE_ITEM` y `GRL_MAE_ITEM_DET` tienen datos vigentes.
- [ ] Properties de Artikos configuradas.
- [ ] Properties Procurement configuradas.
- [ ] `artikos.source.mode=remote`.
- [ ] `artikos.confirm.enabled=true`.
- [ ] `artikos.result.enabled=true`.
- [ ] `procurement.integration.enabled=true`.
- [ ] `procurement.client.enabled=true`.
- [ ] Endpoints dev/admin apagados.
- [ ] Profile definido: `GENERALES` o `VIDA`.
- [ ] Resultado esperado definido: OK, NOK funcional o duplicado idempotente.
- [ ] Equipo operativo listo para monitorear logs y Oracle.

## Request de ejecucion

Endpoint:

```http
POST /api/v1/nominas/batch/start
```

Body ejemplo:

```json
{
  "profile": "GENERALES",
  "dryRun": false
}
```

PowerShell:

```powershell
$body = @{
  profile = "GENERALES"
  dryRun = $false
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/nominas/batch/start" `
  -ContentType "application/json" `
  -Body $body
```

Notas:

- El batch consulta Artikos hasta que `NOMFACTERP` responda que no hay mas nominas.
- `maxNominas` es solo un safety limit operativo.
- La nomina sera confirmada en Artikos con `NOMFACTCONFIR`.
- El resultado sera enviado a Artikos con `NOMFACTRES`.

Para limitar la primera ejecucion:

```json
{
  "profile": "GENERALES",
  "maxNominas": 1,
  "dryRun": false
}
```

## Validaciones durante la ejecucion

Buscar logs por `jobExecutionId`, `profile` y `numeroNomina`.

Debe observarse:

- `sourceMode=remote`.
- `NOMFACTERP` recibido o no hay nominas disponibles.
- `NOMFACTCONFIR` enviado y aceptado.
- `operation=PROCUREMENT_POST_DOCUMENT` por documento.
- Resultado Procurement `statusCode=0`, `-20` idempotente o NOK funcional.
- `NOMFACTRES` enviado y aceptado.
- `CONTROL_NOMINA` actualizado.

No debe observarse:

- tokens completos;
- XML SOAP completo en `INFO`;
- mensajes de skip por `local-xml`;
- `confirmEnabled=false` o `resultEnabled=false`.

## Validaciones despues de la ejecucion

- [ ] Se creo registro en `CONTROL_NOMINA`.
- [ ] `STATUS` final es `OK`, `NOK` o `ERROR`.
- [ ] `TOTAL_DOCUMENTS` correcto.
- [ ] `TOTAL_OK` correcto.
- [ ] `TOTAL_NOK` correcto.
- [ ] Logs muestran `NOMFACTERP` recibido.
- [ ] Logs muestran `NOMFACTCONFIR` enviado/aceptado.
- [ ] Logs muestran Procurement por documento.
- [ ] Logs muestran `NOMFACTRES` enviado/aceptado.
- [ ] Artikos confirma cierre o recepcion del resultado.
- [ ] Procurement tiene documento creado o duplicado controlado.
- [ ] No hay errores tecnicos.

## Queries utiles

Ver `docs/sql-queries.md`, seccion `Validacion ejecucion remota Artikos`.

Consulta rapida:

```sql
SELECT JOB_EXECUTION_ID,
       NUMERO_NOMINA,
       TOTAL_DOCUMENTS,
       TOTAL_OK,
       TOTAL_NOK,
       STATUS,
       ERROR_MESSAGE,
       CREATED_AT,
       UPDATED_AT
FROM CONTROL_NOMINA
ORDER BY CREATED_AT DESC;
```

## Errores esperados y acciones

| Escenario | Sintoma | Accion |
| --- | --- | --- |
| No hay nominas disponibles | Job `COMPLETED` sin nuevas filas `CONTROL_NOMINA`. | Confirmar disponibilidad en Artikos y reintentar solo si corresponde. |
| Rechazo `NOMFACTCONFIR` | `CONTROL_NOMINA=ERROR`, job `FAILED`. | Confirmar estado de la nomina en Artikos. No reprocesar sin autorizacion. |
| Lookup ASI faltante | `PROCUREMENT_MAPPING_ERROR`. | Revisar `GRL_MAE_ITEM_DET`, `GRL_MAE_ITEM`, cuenta, sistema e impuesto. |
| Procurement duplicado | `statusCode=-20`. | Validar que se trate de duplicado esperado; adapter lo informa OK idempotente. |
| Procurement NOK funcional | `TOTAL_NOK > 0`, `CONTROL_NOMINA=NOK`. | Revisar mensaje funcional y confirmar si debe informarse a Artikos como NOK. |
| Procurement tecnico | Job `FAILED`, `CONTROL_NOMINA=ERROR`. | Revisar disponibilidad Procurement, HTTP status, timeout o body no parseable. |
| Rechazo `NOMFACTRES` | Job `FAILED`, `CONTROL_NOMINA=ERROR`. | Confirmar formato y estado esperado en Artikos antes de reintentar. |
| Oracle error | Job `FAILED`. | Revisar conexion, constraints, metadata Batch y `CONTROL_NOMINA`. |

## Criterio de exito

La prueba se considera exitosa si:

- `NOMFACTERP` obtiene la nomina esperada o confirma que no hay nominas disponibles.
- Si hay nomina, `NOMFACTCONFIR` es aceptado.
- Procurement procesa todos los documentos con OK, NOK funcional esperado o duplicado idempotente.
- `NOMFACTRES` es enviado y aceptado por Artikos.
- `CONTROL_NOMINA` queda `OK` o `NOK` funcional esperado.
- El job termina `COMPLETED`.
- No quedan errores tecnicos.

## Rollback y contingencia

No existe rollback automatico del envio a Artikos ni de documentos creados en Procurement.

Acciones de contingencia:

- Detener nuevas ejecuciones del endpoint start.
- Registrar `jobExecutionId`, `profile`, `numeroNomina` y hora.
- Revisar `CONTROL_NOMINA`, `BATCH_JOB_EXECUTION` y logs.
- Coordinar con Artikos si la nomina quedo confirmada pero sin resultado aceptado.
- Coordinar con Procurement si documentos fueron creados parcialmente.
- Reintentar solo con autorizacion y despues de confirmar estado funcional en Artikos y Procurement.

## Paso posterior

Despues de la primera ejecucion, documentar evidencia operativa:

- request start usado;
- `jobExecutionId`;
- numero de nomina;
- status final;
- totales `CONTROL_NOMINA`;
- confirmacion Artikos;
- confirmacion Procurement;
- cualquier observacion funcional.
