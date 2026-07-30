# Infrastructure delivery

Este documento resume la configuracion requerida para desplegar `atk-nomina-batch` en GitLab e Infra.

## Configuration source

La aplicacion espera configuracion por variables de entorno, Azure App Configuration y Azure Key Vault.

- Los valores no sensibles pueden vivir en Azure App Configuration.
- Passwords, tokens Artikos y credenciales de base de datos deben vivir en Azure Key Vault.
- No se deben versionar secretos ni `application-local.properties`.
- QA/PROD deben ejecutar `artikos.source.mode=remote`.

## Required variables

| Variable | Descripcion | Secreto | Ejemplo no sensible |
| --- | --- | --- | --- |
| `DB_URL` | JDBC URL Oracle del servicio | Si | `jdbc:oracle:thin:@//host:1521/service` |
| `DB_USERNAME` | Usuario de servicio Oracle | Si | `ATK_BATCH_SVC` |
| `DB_PASSWORD` | Password del usuario Oracle | Si | `KEY_VAULT_SECRET` |
| `SPRING_BATCH_JDBC_TABLE_PREFIX` | Prefijo/schema de tablas Spring Batch | No | `BACHPROCESS.BATCH_` |
| `PROCUREMENT_BASE_URL` | URL base del servicio Procurement | No | `https://procurement.internal` |
| `PROCUREMENT_INTEGRATION_ENABLED` | Habilita envio real a Procurement | No | `true` |
| `ARTIKOS_NOMINA_URL` | Endpoint Artikos extractor `NOMFACTERP` | No | `https://.../AtkWS_DocExtractorB2B.asmx` |
| `ARTIKOS_CONNECTOR_URL` | Endpoint Artikos connector `NOMFACTCONFIR/NOMFACTRES` | No | `https://.../AtkWS_DocConnectorB2B.asmx` |
| `ARTIKOS_GENERALES_CONSUMO_TOKEN` | Token GENERALES para consumo nomina | Si | `KEY_VAULT_SECRET` |
| `ARTIKOS_GENERALES_RESPUESTA_TOKEN` | Token GENERALES para confirmacion | Si | `KEY_VAULT_SECRET` |
| `ARTIKOS_GENERALES_RESULTADO_TOKEN` | Token GENERALES para resultado | Si | `KEY_VAULT_SECRET` |
| `ARTIKOS_VIDA_CONSUMO_TOKEN` | Token VIDA para consumo nomina | Si | `KEY_VAULT_SECRET` |
| `ARTIKOS_VIDA_RESPUESTA_TOKEN` | Token VIDA para confirmacion | Si | `KEY_VAULT_SECRET` |
| `ARTIKOS_VIDA_RESULTADO_TOKEN` | Token VIDA para resultado | Si | `KEY_VAULT_SECRET` |
| `ARTIKOS_GENERALES_MSG_COD_FROM_ADDRESS` | RUT/codigo emisor GENERALES | No | `REPLACE_ME` |
| `ARTIKOS_GENERALES_MSG_COD_EXTERNO` | Codigo externo GENERALES | No | `REPLACE_ME` |
| `ARTIKOS_VIDA_MSG_COD_FROM_ADDRESS` | RUT/codigo emisor VIDA | No | `REPLACE_ME` |
| `ARTIKOS_VIDA_MSG_COD_EXTERNO` | Codigo externo VIDA | No | `REPLACE_ME` |
| `ATK_BATCH_DEFAULT_MAX_NOMINAS` | Limite default por ejecucion | No | `50` |
| `ATK_BATCH_MAX_NOMINAS_PER_RUN` | Limite maximo permitido | No | `50` |
| `ARTIKOS_HTTP_CONNECT_TIMEOUT_MS` | Timeout conexion SOAP | No | `5000` |
| `ARTIKOS_HTTP_READ_TIMEOUT_MS` | Timeout lectura SOAP | No | `30000` |

## Oracle schemas and service user

La aplicacion usa Oracle para:

- `CONTROL_NOMINA`: control funcional por nomina.
- `GRL_MAE_ITEM`: lookup ASI para Procurement.
- `GRL_MAE_ITEM_DET`: lookup ASI para Procurement.
- `BATCH_*`: metadata tecnica Spring Batch.

Las tablas `BATCH_*` pueden estar en un esquema separado, por ejemplo `BACHPROCESS`. En ese caso configurar:

```properties
SPRING_BATCH_JDBC_TABLE_PREFIX=BACHPROCESS.BATCH_
```

El usuario de servicio debe tener permisos suficientes, idealmente mediante rol corporativo:

| Objeto | Permisos requeridos |
| --- | --- |
| `GRL_MAE_ITEM` | `SELECT` |
| `GRL_MAE_ITEM_DET` | `SELECT` |
| `CONTROL_NOMINA` | `SELECT`, `INSERT`, `UPDATE` |
| `BATCH_JOB_INSTANCE` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` segun politica de purga |
| `BATCH_JOB_EXECUTION` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` segun politica de purga |
| `BATCH_JOB_EXECUTION_PARAMS` | `SELECT`, `INSERT`, `DELETE` segun politica de purga |
| `BATCH_JOB_EXECUTION_CONTEXT` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` segun politica de purga |
| `BATCH_STEP_EXECUTION` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` segun politica de purga |
| `BATCH_STEP_EXECUTION_CONTEXT` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` segun politica de purga |

## Endpoint exposure

Publicar inicialmente solo:

- `POST /api/v1/nominas/batch/start`
- `GET /actuator/health` para monitoreo interno

No publicar:

- `/api/v1/dev/**`
- `/api/v1/admin/**`
- `/api/v1/control-nomina/**`
- `/api/v1/nominas/batch/**` consultas operativas
- Swagger/OpenAPI

## GitLab and deployment TODO

El pipeline fue alineado al modelo `procurement-main`:

- `.gitlab-ci.yml` usa includes corporativos de `shared/zsdevops/cicd/pipelines`.
- `Dockerfile` usa Maven + Java 17, `settings.xml` y certificado Artifactory opcional.
- `settings.xml` no contiene secretos; resuelve usuario/password desde variables de entorno.

Infra debe confirmar:

- Ruta Flux definitiva para `atk-nomina-batch`.
- Repositorio IaC final.
- Componentes corporativos exactos del repo modelo `artikos-integration`.
- Estrategia de inyeccion Azure App Configuration.
- Estrategia de creacion y referencia de secretos Azure Key Vault.
- Nombre de imagen y registry final.
