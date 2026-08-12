# Runbook operativo productivo

## Propósito

`atk-nomina-batch` procesa nóminas de documentos contables disponibles en Artikos. El servicio consulta nóminas mediante `NOMFACTERP`, confirma recepción con `NOMFACTCONFIR`, procesa documentos contra Procurement, envía resultados con `NOMFACTRES` y registra trazabilidad funcional en `CONTROL_NOMINA`.

La metadata técnica del job se mantiene en las tablas Spring Batch `BATCH_*`.

Este documento describe la operación productiva normal. Para incidentes consultar [`support-guide.md`](support-guide.md).

Para configuración, ambientes y ownership consultar [`environments-and-dependencies.md`](environments-and-dependencies.md).

## 1. Contratos productivos expuestos

La exposición normal del servicio debe mantenerse mínima:

- `POST /api/v1/nominas/batch/start`
- `GET /actuator/health`

Los endpoints operativos GET, administración, diagnóstico, `CONTROL_NOMINA` y Swagger no están habilitados por defecto en QA/PROD.

El seguimiento productivo normal se realiza mediante:

```text
jobExecutionId
      |
      v
     logs
      |
      v
BATCH_JOB_EXECUTION
      |
      v
CONTROL_NOMINA
```

Los endpoints operativos REST pueden utilizarse como apoyo adicional únicamente cuando estén habilitados y exista autorización operativa.

## 2. Perfiles soportados

- `VIDA`
- `GENERALES`

Cada perfil posee configuración Artikos independiente.

No se permiten dos ejecuciones simultáneas del mismo perfil. Los perfiles distintos pueden ejecutarse de forma independiente dentro de los límites configurados.

## 3. Flujo funcional resumido

1. Se inicia el batch mediante REST.
2. El reader consulta `NOMFACTERP`.
3. Si Artikos devuelve una nómina, esta se procesa como item completo.
4. Se registra `CONTROL_NOMINA=PROCESSING`.
5. Se ejecuta `NOMFACTCONFIR`.
6. Se validan y transforman los documentos.
7. Cada documento se envía a Procurement.
8. Se construye `NOMFACTRES`.
9. El writer envía `NOMFACTRES`.
10. Se actualiza `CONTROL_NOMINA` con `OK`, `NOK` o `ERROR`.
11. El reader vuelve a consultar Artikos.
12. Cuando Artikos responde que no hay nóminas para procesar, el step termina normalmente.

`maxNominas` es un límite operativo de seguridad. No es la condición funcional normal de término.

## 4. Verificación previa

Antes de iniciar una ejecución productiva:

- verificar `GET /actuator/health`;
- confirmar el perfil solicitado;
- verificar que no exista una ejecución activa inesperada para el mismo perfil;
- confirmar disponibilidad de Oracle;
- confirmar que la configuración del ambiente corresponde al perfil y ambiente esperado;
- ante dudas de endpoints, secretos u ownership, consultar `environments-and-dependencies.md`.

No utilizar credenciales productivas en archivos locales ni modificar configuración para resolver incidentes sin autorización.

## 5. Iniciar el batch

Endpoint:

```http
POST /api/v1/nominas/batch/start
```

Ejemplo:

```json
{
  "profile": "GENERALES",
  "dryRun": false
}
```

Ejemplo con límite operacional:

```json
{
  "profile": "VIDA",
  "maxNominas": 10,
  "dryRun": false
}
```

Respuesta esperada:

```json
{
  "jobExecutionId": 123,
  "jobName": "nominaDocumentosContablesJob",
  "status": "STARTING",
  "message": "Batch iniciado correctamente",
  "profile": "GENERALES",
  "maxNominas": 50,
  "dryRun": false
}
```

Registrar inmediatamente:

- `jobExecutionId`;
- `profile`;
- timestamp aproximado de inicio.

El endpoint responde antes de que el procesamiento termine. El job continúa en segundo plano.

Respuestas operativas relevantes:

- HTTP `409`: ya existe una ejecución activa para el mismo perfil;
- HTTP `400`: parámetros inválidos, incluyendo un `maxNominas` superior al límite permitido.

## 6. Seguimiento productivo

### 6.1 Logs

Los logs utilizan contexto MDC con:

- `jobExecutionId`;
- `profile`;
- `numeroNomina`;
- `operation`.

Operaciones relevantes:

- `NOMFACTERP`
- `NOMFACTCONFIR`
- `PROCUREMENT_POST_DOCUMENT`
- `NOMFACTRES`

El filtro inicial recomendado es:

```text
jobExecutionId=<id>
```

Cuando exista una nómina identificada, complementar con:

```text
numeroNomina=<numero>
operation=<operacion>
```

Los tokens no deben aparecer completos en logs. Si se detecta un token completo, tratarlo como incidente de seguridad.

### 6.2 Metadata Spring Batch

Consultar `BATCH_JOB_EXECUTION` para determinar el estado técnico del job.

Revisar principalmente:

- `STATUS`;
- `EXIT_CODE`;
- `EXIT_MESSAGE`;
- `START_TIME`;
- `END_TIME`.

Las tablas principales son:

- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_JOB_EXECUTION_PARAMS`
- `BATCH_JOB_EXECUTION_CONTEXT`
- `BATCH_STEP_EXECUTION`
- `BATCH_STEP_EXECUTION_CONTEXT`

Las consultas recomendadas están en [`sql-queries.md`](sql-queries.md).

### 6.3 CONTROL_NOMINA

`CONTROL_NOMINA` representa el estado funcional de cada nómina.

Revisar:

- `JOB_EXECUTION_ID`;
- `NUMERO_NOMINA`;
- totales;
- `STATUS`;
- `ERROR_MESSAGE`;
- timestamps.

Estados:

- `PROCESSING`: nómina tomada para procesamiento;
- `OK`: nómina procesada correctamente;
- `NOK`: procesamiento completado con documentos funcionalmente NOK;
- `ERROR`: la nómina no pudo cerrarse correctamente por error técnico o rechazo que interrumpió el flujo.

## 7. Interpretación de estados

| Capa | Estado | Significado |
| --- | --- | --- |
| Spring Batch | `STARTING` | Job solicitado y comenzando. |
| Spring Batch | `STARTED` | Job ejecutándose. |
| Spring Batch | `COMPLETED` | Job terminó sin error técnico. Puede haber nóminas `NOK`. |
| Spring Batch | `FAILED` | El job terminó por una falla técnica o rechazo que interrumpió el procesamiento. |
| Spring Batch | `STOPPING` / `STOPPED` | Detención controlada. |
| `CONTROL_NOMINA` | `OK` | Nómina funcionalmente correcta. |
| `CONTROL_NOMINA` | `NOK` | Nómina finalizada con documentos rechazados/observados. |
| `CONTROL_NOMINA` | `ERROR` | Nómina no cerrada correctamente. |
| Artikos | `MsgStatus=0` | Operación aceptada. |
| Artikos | `MsgStatus!=0` | Rechazo u observación funcional; revisar mensaje Artikos. |

Un job `COMPLETED` y una nómina `NOK` no son contradictorios: Spring Batch refleja el resultado técnico de la ejecución, mientras `CONTROL_NOMINA` refleja el resultado funcional de cada nómina.

## 8. Ruta productiva de diagnóstico inicial

Ante una falla o resultado inesperado:

1. Registrar `jobExecutionId`, `profile`, `numeroNomina` si existe y timestamp.
2. Buscar logs por `jobExecutionId`.
3. Consultar `BATCH_JOB_EXECUTION` y revisar `STATUS`, `EXIT_CODE` y `EXIT_MESSAGE`.
4. Consultar `CONTROL_NOMINA` para el mismo `jobExecutionId`.
5. Identificar la última `operation` relevante.
6. Clasificar la falla:
   - plataforma / gateway / Kubernetes;
   - Spring Batch;
   - `NOMFACTERP`;
   - `NOMFACTCONFIR`;
   - Procurement;
   - `NOMFACTRES`;
   - Oracle / ASI;
   - parsing / mapping / procesamiento interno;
   - resultado funcional de documentos.
7. Aplicar el escenario correspondiente de [`support-guide.md`](support-guide.md).

Si `app.endpoints.operations.enabled=true`, los endpoints REST operativos pueden utilizarse como evidencia complementaria, pero no son requisito para soporte productivo.

## 9. Reglas de reintento

Nunca decidir un reintento únicamente porque el job quedó `FAILED`.

Antes de reintentar determinar hasta qué punto llegó la nómina:

```text
¿Se obtuvo la nómina?
        |
        v
¿Se ejecutó NOMFACTCONFIR?
        |
        v
¿Se enviaron documentos a Procurement?
        |
        v
¿Se intentó NOMFACTRES?
```

Reglas:

- no reenviar una nómina sin conocer su estado actual en Artikos;
- revisar especialmente cualquier ejecución que alcanzó `NOMFACTCONFIR`;
- si hubo llamadas Procurement, considerar efectos parciales antes de reintentar;
- si se intentó `NOMFACTRES`, validar el estado Artikos antes de reenviar;
- no modificar `CONTROL_NOMINA` como mecanismo normal de recuperación;
- no eliminar `BATCH_*` para forzar un reintento;
- distinguir retry técnico automático de reintento operativo/manual.

## 10. Procurement

Para diagnóstico de Procurement filtrar:

```text
jobExecutionId=<id>
operation=PROCUREMENT_POST_DOCUMENT
```

Interpretación funcional conocida:

- `statusCode=0`: documento OK;
- `statusCode=-20` con mensaje de duplicado: éxito idempotente; se informa como OK;
- otro `statusCode` funcional: documento NOK;
- timeout, HTTP `5xx`, respuesta no parseable o error de lookup/mapping: puede llevar la nómina a `ERROR` y el job a `FAILED`.

Si Procurement respondió satisfactoriamente pero el documento no aparece en ASI, no asumir falla del adapter. La investigación debe continuar en Procurement/ASI según ownership documentado.

## 11. Endpoints operativos opcionales

Cuando estén habilitados mediante:

```properties
app.endpoints.operations.enabled=true
```

pueden utilizarse como apoyo:

```http
GET /api/v1/nominas/batch/{jobExecutionId}
GET /api/v1/nominas/batch/{jobExecutionId}/summary
GET /api/v1/nominas/batch/{jobExecutionId}/results/{numeroNomina}
GET /api/v1/control-nomina/jobs/{jobExecutionId}
GET /api/v1/control-nomina/jobs/{jobExecutionId}/nominas/{numeroNomina}
```

No deben habilitarse permanentemente en producción sin autorización y controles correspondientes.

## 12. Operaciones auxiliares y pruebas controladas

### Modo local XML

Para pruebas locales/controladas puede utilizarse:

```properties
artikos.source.mode=local-xml
```

En este modo no se consulta `NOMFACTERP`, no se confirma `NOMFACTCONFIR` y no se envía `NOMFACTRES` real a Artikos.

Consultar:

- [`local-e2e-testing.md`](local-e2e-testing.md)
- [`artikos-replay-local.md`](artikos-replay-local.md)

### Replay antes de una prueba remota

Antes de ejecutar una nómina real en modo remoto completo se recomienda validar previamente un replay sanitizado/controlado cuando corresponda.

El procedimiento completo está en [`artikos-replay-local.md`](artikos-replay-local.md).

### Ejecución remota controlada

Para una validación remota real consultar [`artikos-remote-e2e.md`](artikos-remote-e2e.md).

No utilizar procedimientos de prueba como sustituto de la operación productiva normal.

## 13. Operación administrativa: purga de metadata

La purga de metadata Spring Batch es una operación administrativa independiente. No debe utilizarse para recuperar una nómina ni resolver un incidente funcional.

Endpoint:

```http
POST /api/v1/admin/batch-metadata/purge
```

Solo se carga si:

```properties
app.admin.enabled=true
```

Ejecutar siempre primero en simulación:

```json
{
  "retentionDays": 30,
  "dryRun": true,
  "includeFailed": false
}
```

Solo después de revisar el resultado y contar con autorización:

```json
{
  "retentionDays": 30,
  "dryRun": false,
  "includeFailed": false
}
```

Reglas:

- nunca purgar ejecuciones activas;
- por defecto no purgar `FAILED`;
- considerar `FAILED` solo con `includeFailed=true` y autorización;
- la purga afecta únicamente `BATCH_*`;
- no elimina `CONTROL_NOMINA`;
- no revierte estados Artikos;
- no revierte efectos ya ejecutados en Procurement/ASI.

## 14. Seguridad

- No versionar `application-local.properties`.
- No subir tokens, passwords ni connection strings sensibles.
- Mantener `app.diagnostics.enabled=false` en producción.
- Mantener `app.admin.enabled=false` salvo autorización y control de acceso.
- No copiar secretos en tickets, evidencias o logs.
- Consultar `environments-and-dependencies.md` para clasificación y ubicación de configuración.

## 15. Checklist de operación

- [ ] Verificar `/actuator/health`.
- [ ] Confirmar perfil a ejecutar.
- [ ] Confirmar que no exista una ejecución activa inesperada para el mismo perfil.
- [ ] Confirmar disponibilidad de Oracle.
- [ ] Iniciar el batch.
- [ ] Registrar `jobExecutionId`, `profile` y timestamp.
- [ ] Monitorear logs por `jobExecutionId`.
- [ ] Consultar `BATCH_JOB_EXECUTION` y confirmar estado técnico final.
- [ ] Revisar `CONTROL_NOMINA` para identificar `OK`, `NOK` y `ERROR`.
- [ ] Si el job está `FAILED` o existe una nómina `ERROR`, aplicar `support-guide.md`.
- [ ] Documentar cualquier intervención o escalamiento realizado.

## 16. Checklist post-incidente

- [ ] Registrar `jobExecutionId`, `profile`, `numeroNomina` y timestamp.
- [ ] Revisar logs por `jobExecutionId`.
- [ ] Revisar `BATCH_JOB_EXECUTION.EXIT_MESSAGE`.
- [ ] Revisar `CONTROL_NOMINA`.
- [ ] Identificar la operación fallida.
- [ ] Confirmar si hubo retry técnico.
- [ ] Determinar si hubo efectos en Artikos o Procurement.
- [ ] No reintentar hasta conocer el estado externo de la nómina cuando corresponda.
- [ ] Escalar utilizando la evidencia mínima definida en `support-guide.md`.
- [ ] Documentar la acción tomada y el resultado.
