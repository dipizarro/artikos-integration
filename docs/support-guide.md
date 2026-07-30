# Guia de soporte

## Escenario 1: Artikos responde "No hay nominas para procesar"

### Significado

Artikos respondio correctamente a `NOMFACTERP`, pero no tiene nominas disponibles para el perfil consultado.

### Estado esperado

- Job Spring Batch: `COMPLETED`.
- `CONTROL_NOMINA`: sin nuevas filas si no se proceso ninguna nomina.

### Accion requerida

- No es error.
- Registrar que la cola estaba vacia.
- Si negocio esperaba nominas, validar disponibilidad directamente con Artikos o con el equipo funcional.

## Escenario 2: Falla NOMFACTERP

### Posibles causas

- Endpoint Artikos no disponible.
- Timeout de red.
- DNS o conectividad.
- Token o configuracion incorrecta.
- Error HTTP `5xx` persistente.
- Respuesta SOAP no parseable.

### Accion sugerida

1. Revisar logs con `operation=NOMFACTERP`.
2. Confirmar si hubo retry tecnico.
3. Revisar `/actuator/health`.
4. Validar propiedades Artikos para el perfil.
5. Revisar conectividad desde el ambiente.
6. Si el error es funcional Artikos, revisar `MessageOut.LogMessage.MessageText`.

### Estado esperado

- Job Spring Batch: `FAILED`.
- `CONTROL_NOMINA`: normalmente sin fila para esa nomina si la nomina no pudo obtenerse.

## Escenario 3: Falla NOMFACTCONFIR

### Posibles causas

- La nomina no esta en estado Artikos valido para confirmar.
- Token incorrecto para el perfil.
- Perfil o `MsgFromAddress` incorrecto.
- XML de confirmacion rechazado.
- Artikos devuelve `MsgStatus != 0`.

### Accion sugerida

1. Revisar logs con `operation=NOMFACTCONFIR`.
2. Revisar `CONTROL_NOMINA` para la nomina.
3. Revisar `MessageOut.LogMessage.MessageText`.
4. Confirmar el estado de la nomina en Artikos.
5. No reintentar hasta validar estado funcional con Artikos.

### Estado esperado

- Job Spring Batch: `FAILED`.
- `CONTROL_NOMINA`: `ERROR`.

## Escenario 4: Falla NOMFACTRES

### Posibles causas

- La nomina no esta en estado Artikos valido para recibir resultado.
- XML resultado invalido.
- Datos de documento inconsistentes.
- Token o perfil incorrecto.
- Artikos devuelve `MsgStatus != 0`.
- Falla tecnica de red despues de procesar la nomina.

### Accion sugerida

1. Revisar logs con `operation=NOMFACTRES`.
2. Revisar `CONTROL_NOMINA`.
3. Revisar el resultado funcional por endpoint si esta disponible.
4. Validar `MessageOut.LogMessage.MessageText`.
5. Confirmar estado de la nomina en Artikos antes de reintentar.
6. Si el error indica formato XML, revisar generacion de `NOMFACTRES`.

### Estado esperado

- Job Spring Batch: `FAILED`.
- `CONTROL_NOMINA`: `ERROR`.

## Escenario 5: Falla Oracle

### Posibles causas

- Datasource mal configurado.
- Password vencida o cuenta bloqueada.
- Tabla `CONTROL_NOMINA` inexistente o con estructura distinta.
- Tablas `BATCH_*` inexistentes.
- Problemas de permisos.
- Problemas de espacio o bloqueo en Oracle.

### Accion sugerida

1. Revisar logs de Hikari, JPA y Spring Batch.
2. Confirmar que la app esta usando el perfil correcto.
3. Validar URL, usuario y schema Oracle.
4. Validar existencia de `CONTROL_NOMINA`.
5. Validar existencia de tablas `BATCH_*`.
6. Revisar permisos de insert, update, select y delete segun corresponda.

### Estado esperado

- Job Spring Batch: `FAILED` si la falla ocurre durante ejecucion.
- App puede no levantar si falla validacion JPA o datasource inicial.

## Escenario 6: Job queda FAILED

### Accion sugerida

1. Consultar:

```http
GET /api/v1/nominas/batch/{jobExecutionId}
```

2. Revisar SQL:

```sql
SELECT JOB_EXECUTION_ID, STATUS, EXIT_CODE, EXIT_MESSAGE
FROM BATCH_JOB_EXECUTION
WHERE JOB_EXECUTION_ID = :jobExecutionId;
```

3. Revisar `CONTROL_NOMINA`:

```sql
SELECT *
FROM CONTROL_NOMINA
WHERE JOB_EXECUTION_ID = :jobExecutionId
ORDER BY NUMERO_NOMINA;
```

4. Revisar logs con:

```text
jobExecutionId={jobExecutionId}
```

5. Determinar la operacion fallida:

- `NOMFACTERP`
- `NOMFACTCONFIR`
- `NOMFACTRES`
- Oracle
- procesamiento interno

### Accion de cierre

- Si es error funcional Artikos, coordinar con equipo funcional antes de reintentar.
- Si es error tecnico transitorio, reintentar solo despues de confirmar estado de nominas parcialmente procesadas.
- Si hay `CONTROL_NOMINA.ERROR`, registrar `ERROR_MESSAGE`.

## Escenario 7: No se puede iniciar job porque ya hay ejecucion activa

### Sintoma

`POST /api/v1/nominas/batch/start` responde HTTP `409`.

### Significado

Ya existe una ejecucion activa para el mismo perfil. La aplicacion permite concurrencia entre perfiles distintos, pero no permite dos ejecuciones simultaneas del mismo perfil.

### Accion sugerida

1. Revisar jobs activos:

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

2. Esperar termino normal si el job esta avanzando.
3. Revisar logs si el job parece detenido.
4. No lanzar otro job para el mismo perfil hasta aclarar el estado.

## Escenario 8: Se alcanza maxNominas

### Significado

El batch llego al limite operativo configurado o solicitado. No significa que la cola Artikos este vacia.

### Estado esperado

- Job Spring Batch: `COMPLETED`.
- Logs: mensaje indicando limite operacional alcanzado.
- `CONTROL_NOMINA`: filas de las nominas procesadas hasta el limite.

### Accion sugerida

- Revisar summary.
- Si se requiere continuar, lanzar una nueva ejecucion controlada.
- Ajustar `maxNominas` solo dentro del limite `atk.batch.max-nominas-per-run`.

## Escenario 9: Health de configuracion Artikos degradado

### Significado

El health indicator detecto configuracion faltante. No realiza llamadas a Artikos.

### Accion sugerida

1. Revisar `/actuator/health`.
2. Revisar detalles del indicador Artikos.
3. Validar variables de entorno o properties del perfil activo.
4. Confirmar que existen configuraciones para `VIDA` y `GENERALES`.
5. Confirmar que las operaciones `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES` estan configuradas.
