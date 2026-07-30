# Arquitectura

## Objetivo

`atk-nomina-batch` es un servicio de integracion batch para procesar nominas de documentos contables Artikos usando Spring Boot 3, Spring Batch, SOAP y Oracle.

El sistema opera como puente entre Artikos QA y el procesamiento interno de nominas:

- consulta nominas disponibles con `NOMFACTERP`;
- confirma recepcion con `NOMFACTCONFIR`;
- procesa documentos, conciliaciones y distribuciones;
- genera y envia resultado con `NOMFACTRES`;
- registra control funcional en `CONTROL_NOMINA`;
- mantiene metadata tecnica en tablas `BATCH_*` de Spring Batch.

## Componentes

- API REST: expone endpoints operacionales bajo `/api/v1`.
- Batch: concentra configuracion, readers, processors y writers de Spring Batch.
- SOAP Artikos: encapsula construccion de requests, consumo HTTP y parseo de respuestas.
- Dominio: contiene modelos de nomina, documentos, resultados y configuracion Artikos.
- Persistencia: usa JPA para `CONTROL_NOMINA` y JdbcTemplate para mantenimiento de metadata `BATCH_*`.
- Shared exception: centraliza excepciones de parsing e integracion.

## Paquete base

El paquete Java base es `cl.atk.nomina.batch`. La migracion desde el namespace historico quedo registrada en `docs/decisions/ADR-002-package-namespace.md`.

## Disparador REST

El endpoint `POST /api/v1/nominas/batch/start` actua como interruptor del job `nominaDocumentosContablesJob`. El lanzamiento usa un `JobLauncher` con `TaskExecutor`, por lo que la respuesta se entrega inmediatamente con el `jobExecutionId` mientras el step `processNominaDocumentosStep` continua ejecutandose en segundo plano.

Endpoints de consulta:

- `GET /api/v1/nominas/batch/{jobExecutionId}` consulta metadata Spring Batch mediante `JobExplorer`.
- `GET /api/v1/nominas/batch/{jobExecutionId}/summary` expone totales funcionales.
- `GET /api/v1/nominas/batch/{jobExecutionId}/results/{numeroNomina}` expone resultado por nomina y XML `NOMFACTRES`.

## Procesamiento chunk

El job procesa nominas completas mediante `ItemReader`, `ItemProcessor` e `ItemWriter`.

- Reader real: consulta Artikos QA con `NOMFACTERP`, parsea la respuesta SOAP y entrega una nomina por item.
- Processor real: registra `CONTROL_NOMINA` en `PROCESSING`, confirma recepcion con `NOMFACTCONFIR` cuando `dryRun=false`, procesa documentos y genera el XML `NOMFACTRES`.
- Writer real: envia `NOMFACTRES` cuando `dryRun=false`, actualiza `CONTROL_NOMINA` con `OK`, `NOK` o `ERROR`, y agrega resultados por `jobExecutionId` y `numeroNomina` al store en memoria.
- Componentes simulados: se conservan para pruebas locales y seran revisados en la limpieza productiva.

Para SOAP real, el tamano de chunk se configura con `atk.batch.real.chunk-size` y se recomienda `1`, porque cada nomina implica confirmacion y envio de resultado.

## Estados funcionales Artikos

Las pruebas contra Artikos QA y SoapUI mostraron que `NOMFACTERP` puede seguir devolviendo una nomina aunque no este apta para avanzar en las operaciones siguientes.

Validaciones funcionales observadas:

- `NOMFACTCONFIR` requiere que la nomina este en estado `En Integracion`.
- `NOMFACTRES` requiere que la nomina este en estado `Recibida`.

Si Artikos rechaza una operacion por estado, la respuesta llega con `MsgStatus=1` y debe registrarse como error funcional en `CONTROL_NOMINA`.

La decision queda documentada en:

```text
docs/decisions/ADR-003-artikos-nomina-state-transitions.md
```

## Persistencia y metadata batch

En ejecucion local contra Oracle, la aplicacion usa `spring.batch.jdbc.initialize-schema=never`. Las tablas de metadata `BATCH_*` deben existir antes de invocar `POST /api/v1/nominas/batch/start`, ejecutando:

```text
src/main/resources/db/oracle/V000__create_spring_batch_metadata.sql
```

La tabla funcional de control por nomina se crea con:

```text
src/main/resources/db/oracle/V001__create_control_nomina.sql
```

La purga controlada de metadata Spring Batch opera solo sobre `BATCH_*` y no elimina registros de `CONTROL_NOMINA`.
