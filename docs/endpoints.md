# Endpoints

## Gateway contract

El contrato productivo inicial expuesto por CONC/Kong es:

| Metodo | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/v1/nominas/batch/start` | Dispara asincronicamente el batch desde un sistema externo |
| `GET` | `/actuator/health` | Health check para monitoreo interno |

La autenticacion y autorizacion se gestionan en el gateway corporativo. La aplicacion no implementa seguridad propia pesada en este alcance.

## Productive Endpoints

Estos endpoints forman parte de la superficie REST de la aplicacion. En QA/PROD, por defecto solo `POST /api/v1/nominas/batch/start` y `GET /actuator/health` quedan disponibles para la entrega inicial.

| Metodo | Ruta | Uso |
| --- | --- | --- |
| `GET` | `/api/v1/health` | Health propio de la aplicacion; no publicar inicialmente |
| `POST` | `/api/v1/nominas/batch/start` | Inicia asincronicamente el job de nominas |
| `GET` | `/api/v1/nominas/batch/{jobExecutionId}` | Consulta estado Spring Batch; requiere `app.endpoints.operations.enabled=true` |
| `GET` | `/api/v1/nominas/batch/{jobExecutionId}/summary` | Consulta resumen funcional del job; requiere `app.endpoints.operations.enabled=true` |
| `GET` | `/api/v1/nominas/batch/{jobExecutionId}/results/{numeroNomina}` | Consulta resultado funcional por nomina; requiere `app.endpoints.operations.enabled=true` |
| `GET` | `/api/v1/control-nomina/jobs/{jobExecutionId}` | Consulta registros `CONTROL_NOMINA` por job; requiere `app.endpoints.operations.enabled=true` |
| `GET` | `/api/v1/control-nomina/jobs/{jobExecutionId}/nominas/{numeroNomina}` | Consulta un registro `CONTROL_NOMINA` especifico; requiere `app.endpoints.operations.enabled=true` |
| `POST` | `/api/v1/admin/batch-metadata/purge` | Simula o ejecuta purga controlada de metadata `BATCH_*`; requiere `app.admin.enabled=true` |
| `GET` | `/actuator/health` | Health Spring Actuator |
| `GET` | `/swagger-ui.html` | Documentacion OpenAPI; deshabilitada en QA/PROD |
| `GET` | `/v3/api-docs` | Especificacion OpenAPI; deshabilitada en QA/PROD |

Los endpoints operativos de consulta solo se cargan si:

```properties
app.endpoints.operations.enabled=true
```

El endpoint de purga solo se carga si `app.admin.enabled=true`. Ademas, debe protegerse con autenticacion y autorizacion antes de uso productivo.

En QA/PROD la documentacion OpenAPI queda apagada por defecto:

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

## Productive batch usage

Ejemplo de inicio operacional:

```http
POST /api/v1/nominas/batch/start
Content-Type: application/json
```

```json
{
  "profile": "GENERALES",
  "dryRun": false
}
```

El batch consulta `NOMFACTERP` hasta que Artikos responde que no hay nominas para procesar. `maxNominas` puede informarse en el request, pero solo actua como limite de seguridad; no es la condicion funcional de termino. Cada nomina procesada genera una fila en `CONTROL_NOMINA` y un envio `NOMFACTRES`.

## Diagnostic Endpoints

Los endpoints diagnosticos solo se cargan si:

```properties
app.diagnostics.enabled=true
```

Por defecto la propiedad esta deshabilitada en `application.properties`. Puede habilitarse en `application-local.properties` o en ambientes QA controlados. No debe estar activa en produccion.

| Metodo | Ruta | Uso |
| --- | --- | --- |
| `POST` | `/api/v1/dev/artikos/nominas/fetch` | Prueba directa `NOMFACTERP` |
| `POST` | `/api/v1/dev/artikos/nominas/confirm` | Prueba directa `NOMFACTCONFIR` |
| `POST` | `/api/v1/dev/artikos/nominas/result/test` | Prueba directa `NOMFACTRES` con payload manual |
| `POST` | `/api/v1/dev/procurement/documents/test` | Prueba mapeo Artikos XML -> Procurement CMP y envia un documento |
| `GET` | `/api/v1/dev/artikos/config/{profile}` | Muestra configuracion Artikos enmascarada |
| `POST` | `/api/v1/dev/control-nomina/test` | Inserta y actualiza un registro diagnostico en `CONTROL_NOMINA` |

La configuracion enmascarada nunca debe exponer tokens completos. Los valores sensibles deben salir solo como presencia y mascara parcial.

### Procurement diagnostic usage

Este endpoint no confirma nomina en Artikos, no escribe `CONTROL_NOMINA` y no envia `NOMFACTRES`. Solo toma el XML SOAP local configurado en `atk.batch.sample-file` o un `rawXml` enviado en el request, selecciona un documento, lo mapea a CMP y llama Procurement.

```http
POST /api/v1/dev/procurement/documents/test
Content-Type: application/json
```

```json
{
  "profile": "VIDA",
  "documentIndex": 0
}
```

Para usar un XML SOAP capturado, enviar `rawXml` como string JSON. Si se omite, se usa `atk.batch.sample-file`.
