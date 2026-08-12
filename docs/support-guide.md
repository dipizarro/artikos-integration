# Guía de soporte productivo

## Propósito

Este documento describe cómo diagnosticar incidentes de `atk-nomina-batch` en operación productiva.

La secuencia base de diagnóstico es:

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
      |
      v
operación fallida
      |
      v
escenario de soporte
```

Los endpoints REST operativos son opcionales y solo deben utilizarse cuando estén habilitados.

Para operación normal consultar [`runbook.md`](runbook.md). Para configuración, ambientes y ownership consultar [`environments-and-dependencies.md`](environments-and-dependencies.md).

## 1. Evidencia mínima de incidente

Antes de escalar, registrar cuando esté disponible:

- `jobExecutionId`;
- `profile`;
- `numeroNomina`;
- timestamp aproximado;
- `operation` fallida;
- estado Spring Batch;
- estado `CONTROL_NOMINA`;
- `EXIT_MESSAGE`;
- mensaje funcional del sistema externo;
- resultado de `/actuator/health` cuando sea relevante.

Para Procurement agregar, cuando corresponda:

- documento/folio;
- HTTP status;
- `statusCode` funcional.

No incluir tokens, passwords ni secretos en tickets o evidencias.

## 2. Matriz de triage

| Síntoma | Revisar primero | Responsable probable | Acción segura |
| --- | --- | --- | --- |
| API no disponible | `/actuator/health`, pod, gateway | Infra / aplicación | validar despliegue y conectividad |
| Job `FAILED` | logs + `BATCH_JOB_EXECUTION` | según `operation` | clasificar antes de reintentar |
| Nómina `ERROR` | `CONTROL_NOMINA` + logs | integración / dependencia externa | revisar último efecto externo |
| Nómina `NOK` | resultado funcional/documentos | funcional / Procurement | revisar documentos NOK |
| Falla `NOMFACTERP` | logs SOAP | Artikos / integración | validar configuración y conectividad |
| Falla `NOMFACTCONFIR` | estado Artikos | Artikos / integración | no reintentar sin validar estado |
| Falla Procurement | `PROCUREMENT_POST_DOCUMENT` | Procurement / integración | revisar respuesta y efectos parciales |
| Falla `NOMFACTRES` | estado Artikos | Artikos / integración | validar antes de reenviar |
| Falla Oracle | datasource / permisos / objetos | DBA / ASI | validar conectividad y permisos |

## 3. Regla general de reintento

Nunca decidir un reintento únicamente porque el job quedó `FAILED`.

Determinar primero hasta qué punto avanzó la nómina:

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
- si se ejecutó `NOMFACTCONFIR`, validar especialmente el estado funcional antes de reintentar;
- si hubo llamadas Procurement, revisar posibles efectos parciales;
- si se intentó `NOMFACTRES`, validar el estado Artikos antes de reenviar;
- no modificar `CONTROL_NOMINA` para forzar recuperación;
- no eliminar metadata `BATCH_*` para forzar reintento;
- distinguir retry técnico automático de reintento operativo/manual.

## 4. Escenario: Artikos responde "No hay nóminas para procesar"

### Significado

Artikos respondió correctamente a `NOMFACTERP`, pero no existen nóminas disponibles para el perfil consultado.

### Estado esperado

- Spring Batch: `COMPLETED`.
- `CONTROL_NOMINA`: sin nuevas filas si no se procesó ninguna nómina.

### Acción

- No es error.
- Registrar que la cola estaba vacía.
- Si negocio esperaba nóminas, validar disponibilidad con Artikos/equipo funcional.

## 5. Escenario: falla NOMFACTERP

### Posibles causas

- endpoint Artikos no disponible;
- timeout;
- DNS/conectividad;
- token o configuración incorrecta;
- HTTP `5xx` persistente;
- respuesta SOAP no parseable;
- rechazo funcional Artikos.

### Acción

1. Revisar logs con `operation=NOMFACTERP`.
2. Confirmar si hubo retry técnico.
3. Revisar `/actuator/health`.
4. Validar configuración del perfil en `environments-and-dependencies.md`.
5. Revisar conectividad desde el ambiente.
6. Si existe respuesta funcional, revisar `MessageOut.LogMessage.MessageText`.

### Estado esperado

- Spring Batch: normalmente `FAILED`.
- `CONTROL_NOMINA`: normalmente sin fila si nunca se obtuvo una nómina.

## 6. Escenario: falla NOMFACTCONFIR

### Posibles causas

- nómina en estado Artikos no válido;
- token/perfil incorrecto;
- `MsgFromAddress` u otro parámetro incorrecto;
- XML rechazado;
- `MsgStatus != 0`;
- error técnico de red.

### Acción

1. Revisar logs con `operation=NOMFACTCONFIR`.
2. Revisar `CONTROL_NOMINA` para la nómina.
3. Revisar `MessageOut.LogMessage.MessageText`.
4. Confirmar el estado actual de la nómina en Artikos.
5. No reintentar hasta validar el estado funcional.

### Estado esperado

- Spring Batch: `FAILED`.
- `CONTROL_NOMINA`: `ERROR`.

## 7. Escenario: falla Procurement

### Identificación

Filtrar logs por:

```text
jobExecutionId=<id>
operation=PROCUREMENT_POST_DOCUMENT
```

### Timeout / conectividad

- No existe una respuesta confiable del servicio.
- La nómina puede quedar `ERROR` y el job `FAILED`.
- Antes de reintentar, confirmar si Procurement alcanzó a producir efectos parciales.

### HTTP 5xx

- Falla técnica del servicio Procurement o su plataforma.
- Registrar HTTP status, documento y timestamp.
- Validar disponibilidad del servicio y escalar a Procurement cuando corresponda.

### Respuesta no parseable

- La integración recibió una respuesta incompatible con el contrato esperado.
- Revisar respuesta sanitizada y logs.
- No alterar mapping ni contrato durante el incidente sin una corrección trazable.

### Rechazo funcional

- Procurement respondió técnicamente, pero rechazó el documento.
- Un rechazo funcional puede producir documento `NOK` sin que necesariamente falle todo el job.
- Registrar el `statusCode` y mensaje funcional.

### Duplicado / idempotencia

La implementación considera:

- `statusCode=0`: OK;
- `statusCode=-20` con mensaje de duplicado: OK idempotente;
- otro `statusCode` funcional: NOK.

Un duplicado reconocido no debe tratarse automáticamente como incidente.

### No hubo llamada Procurement

Revisar:

- flags de integración;
- configuración del ambiente;
- si el flujo llegó al mapping;
- existencia de `operation=PROCUREMENT_POST_DOCUMENT`.

Consultar `environments-and-dependencies.md` para valores/ownership sin copiar secretos.

### Procurement respondió OK pero el documento no aparece en ASI

No asumir que falló `atk-nomina-batch`.

Si existe evidencia de POST aceptado por Procurement, la investigación debe continuar en Procurement/ASI. El adapter no persiste directamente los documentos contables finales en ASI.

## 8. Escenario: falla NOMFACTRES

### Posibles causas

- nómina en estado Artikos no válido para recibir resultado;
- XML resultado inválido;
- datos inconsistentes;
- token/perfil incorrecto;
- `MsgStatus != 0`;
- falla técnica después de procesar documentos.

### Acción

1. Revisar logs con `operation=NOMFACTRES`.
2. Revisar `CONTROL_NOMINA`.
3. Validar mensaje funcional Artikos.
4. Confirmar estado actual de la nómina en Artikos antes de reintentar.
5. Si corresponde a formato XML, revisar generación de `NOMFACTRES` mediante cambio trazable.

### Estado esperado

- Spring Batch: `FAILED`.
- `CONTROL_NOMINA`: `ERROR`.

## 9. Escenario: falla Oracle / ASI

### Posibles causas

- datasource incorrecto;
- password vencida/cuenta bloqueada;
- `CONTROL_NOMINA` inexistente o incompatible;
- `BATCH_*` inexistentes;
- problemas de permisos;
- problemas de espacio/bloqueo;
- lookup `GRL_MAE_ITEM` / `GRL_MAE_ITEM_DET` no disponible.

### Acción

1. Revisar logs Hikari/JPA/Spring Batch.
2. Validar ambiente/configuración.
3. Confirmar URL, usuario y schema.
4. Confirmar existencia y permisos de `CONTROL_NOMINA`.
5. Confirmar metadata `BATCH_*`.
6. Confirmar permisos de lookup ASI.
7. Escalar a DBA/ASI cuando la causa sea de base de datos.

### Estado esperado

- Spring Batch: `FAILED` si ocurre durante ejecución.
- La aplicación puede no iniciar si falla datasource/JPA al startup.

## 10. Escenario: Job queda FAILED

### Acción

1. Buscar logs por `jobExecutionId`.
2. Consultar:

```sql
SELECT JOB_EXECUTION_ID,
       STATUS,
       EXIT_CODE,
       EXIT_MESSAGE,
       START_TIME,
       END_TIME
FROM BATCH_JOB_EXECUTION
WHERE JOB_EXECUTION_ID = :jobExecutionId;
```

3. Consultar:

```sql
SELECT *
FROM CONTROL_NOMINA
WHERE JOB_EXECUTION_ID = :jobExecutionId
ORDER BY NUMERO_NOMINA;
```

4. Identificar la última operación relevante:

- `NOMFACTERP`;
- `NOMFACTCONFIR`;
- `PROCUREMENT_POST_DOCUMENT`;
- `NOMFACTRES`;
- Oracle;
- procesamiento interno.

5. Aplicar el escenario específico.
6. Reintentar solo después de conocer posibles efectos externos.

Si los endpoints operativos están habilitados, `GET /api/v1/nominas/batch/{jobExecutionId}` puede utilizarse como evidencia adicional, no como requisito.

## 11. Escenario: nómina NOK con job COMPLETED

### Significado

No es necesariamente una contradicción.

- Spring Batch `COMPLETED`: ejecución técnicamente completada.
- `CONTROL_NOMINA=NOK`: existen documentos funcionalmente rechazados/observados dentro de una nómina procesada.

### Acción

1. Identificar documentos NOK.
2. Revisar respuesta Procurement/resultado funcional.
3. Determinar si corresponde a dato, mapping o regla funcional.
4. No reiniciar automáticamente el batch por una nómina `NOK`.

## 12. Escenario: no se puede iniciar porque existe ejecución activa

### Síntoma

`POST /api/v1/nominas/batch/start` responde HTTP `409`.

### Significado

Existe una ejecución activa para el mismo perfil.

### Acción

Consultar jobs activos:

```sql
SELECT BJE.JOB_EXECUTION_ID,
       BJI.JOB_NAME,
       BJE.STATUS,
       BJE.START_TIME
FROM BATCH_JOB_EXECUTION BJE
JOIN BATCH_JOB_INSTANCE BJI
  ON BJI.JOB_INSTANCE_ID = BJE.JOB_INSTANCE_ID
WHERE BJE.STATUS IN ('STARTING', 'STARTED', 'STOPPING')
ORDER BY BJE.START_TIME DESC;
```

- esperar término normal si avanza;
- revisar logs si parece detenido;
- no lanzar otro job del mismo perfil hasta aclarar estado.

## 13. Escenario: se alcanza maxNominas

### Significado

Se alcanzó el límite operacional configurado/solicitado. No significa que Artikos esté sin nóminas.

### Estado esperado

- Spring Batch: `COMPLETED`.
- Logs: límite operacional alcanzado.
- `CONTROL_NOMINA`: filas de las nóminas procesadas hasta el límite.

### Acción

- revisar `BATCH_JOB_EXECUTION` y `CONTROL_NOMINA`;
- si corresponde continuar, iniciar una nueva ejecución controlada;
- respetar `atk.batch.max-nominas-per-run`.

## 14. Escenario: health de configuración Artikos degradado

### Significado

El health indicator detectó configuración faltante. No implica necesariamente que se haya invocado Artikos.

### Acción

1. Revisar `/actuator/health`.
2. Revisar detalles del indicador Artikos cuando estén disponibles.
3. Validar configuración del ambiente/perfil.
4. Confirmar configuración para `VIDA` y `GENERALES`.
5. Confirmar las operaciones `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`.
6. Consultar `environments-and-dependencies.md`.

## 15. Escenario: API no disponible

### Revisar

- `/actuator/health` si responde;
- estado del pod/deployment;
- gateway/routing;
- logs de startup;
- conectividad con Oracle/configuración si la aplicación no levanta.

### Escalamiento

Clasificar si la causa corresponde a:

- aplicación;
- Kubernetes/Infra;
- Gateway;
- configuración/secretos;
- Oracle.

Utilizar `environments-and-dependencies.md` para identificar ownership.

## 16. Escenario: parsing, mapping o procesamiento interno

### Posibles causas

- XML Artikos inesperado;
- dato obligatorio ausente;
- lookup ASI fallido;
- transformación Procurement inválida;
- excepción interna.

### Acción

1. Revisar logs por `jobExecutionId` y `numeroNomina`.
2. Identificar si hubo `NOMFACTCONFIR` antes del error.
3. Identificar si existieron llamadas Procurement.
4. Revisar `CONTROL_NOMINA.ERROR_MESSAGE`.
5. Reproducir mediante replay local sanitizado si corresponde.
6. Cualquier corrección de mapping debe ingresar por Issue, prueba y Pull Request.

## 17. Escalamiento

Antes de escalar confirmar que la evidencia mínima esté reunida y que la dependencia probable esté identificada.

Consultar [`environments-and-dependencies.md`](environments-and-dependencies.md) para ownership de:

- Artikos;
- Procurement;
- Oracle/ASI;
- Infra/Kubernetes;
- Gateway;
- Azure/configuración.

La escalada debe describir hechos observados y evitar afirmar responsabilidades sin evidencia.
