# Infrastructure delivery

Este documento resume la configuración requerida para desplegar `atk-nomina-batch` en la infraestructura del cliente. El ciclo de promoción entre GitHub corporativo y GitLab cliente se documenta en `docs/release-and-deployment.md`.

## Configuration source

La aplicación espera configuración por variables de entorno, Azure App Configuration y Azure Key Vault.

- Los valores no sensibles pueden vivir en Azure App Configuration.
- Passwords, tokens Artikos y credenciales de base de datos deben vivir en Azure Key Vault.
- No se deben versionar secretos ni `application-local.properties`.
- QA/PROD deben ejecutar `artikos.source.mode=remote`.

Para la separación entre configuración, secretos, ambientes y ownership consultar `docs/environments-and-dependencies.md`.

## Required variables

La aplicación utiliza dos datasources Oracle independientes: `APP_DATASOURCE_*` para persistencia funcional/JPA y `BATCH_DATASOURCE_*` para metadata Spring Batch.

| Variable | Descripción | Secreto | Ejemplo no sensible |
| --- | --- | --- | --- |
| `APP_DATASOURCE_URL` | JDBC URL Oracle para aplicación/JPA | Sí | `jdbc:oracle:thin:@//host:1521/service` |
| `APP_DATASOURCE_USERNAME` | Usuario Oracle para aplicación/JPA | Sí | `ATK_APP_SVC` |
| `APP_DATASOURCE_PASSWORD` | Password datasource aplicación/JPA | Sí | `KEY_VAULT_SECRET` |
| `BATCH_DATASOURCE_URL` | JDBC URL Oracle para Spring Batch | Sí | `jdbc:oracle:thin:@//host:1521/service` |
| `BATCH_DATASOURCE_USERNAME` | Usuario Oracle para Spring Batch | Sí | `ATK_BATCH_SVC` |
| `BATCH_DATASOURCE_PASSWORD` | Password datasource Spring Batch | Sí | `KEY_VAULT_SECRET` |
| `APP_DB_SCHEMA` | Schema JPA de aplicación | No | `ASI` |
| `SPRING_BATCH_JDBC_TABLE_PREFIX` | Prefijo/schema de tablas Spring Batch | No | `BACHPROCESS.BATCH_` |
| `PROCUREMENT_BASE_URL` | URL base del servicio Procurement | No | `https://procurement.internal` |
| `PROCUREMENT_INTEGRATION_ENABLED` | Habilita envío real a Procurement | No | `true` |
| `ARTIKOS_NOMINA_URL` | Endpoint Artikos extractor `NOMFACTERP` | No | `https://.../AtkWS_DocExtractorB2B.asmx` |
| `ARTIKOS_CONNECTOR_URL` | Endpoint Artikos connector `NOMFACTCONFIR/NOMFACTRES` | No | `https://.../AtkWS_DocConnectorB2B.asmx` |
| `ARTIKOS_GENERALES_CONSUMO_TOKEN` | Token GENERALES para consumo nómina | Sí | `KEY_VAULT_SECRET` |
| `ARTIKOS_GENERALES_RESPUESTA_TOKEN` | Token GENERALES para confirmación | Sí | `KEY_VAULT_SECRET` |
| `ARTIKOS_GENERALES_RESULTADO_TOKEN` | Token GENERALES para resultado | Sí | `KEY_VAULT_SECRET` |
| `ARTIKOS_VIDA_CONSUMO_TOKEN` | Token VIDA para consumo nómina | Sí | `KEY_VAULT_SECRET` |
| `ARTIKOS_VIDA_RESPUESTA_TOKEN` | Token VIDA para confirmación | Sí | `KEY_VAULT_SECRET` |
| `ARTIKOS_VIDA_RESULTADO_TOKEN` | Token VIDA para resultado | Sí | `KEY_VAULT_SECRET` |
| `ARTIKOS_GENERALES_MSG_COD_FROM_ADDRESS` | RUT/código emisor GENERALES | No | `REPLACE_ME` |
| `ARTIKOS_GENERALES_MSG_COD_EXTERNO` | Código externo GENERALES | No | `REPLACE_ME` |
| `ARTIKOS_VIDA_MSG_COD_FROM_ADDRESS` | RUT/código emisor VIDA | No | `REPLACE_ME` |
| `ARTIKOS_VIDA_MSG_COD_EXTERNO` | Código externo VIDA | No | `REPLACE_ME` |
| `ATK_BATCH_DEFAULT_MAX_NOMINAS` | Límite default por ejecución | No | `50` |
| `ATK_BATCH_MAX_NOMINAS_PER_RUN` | Límite máximo permitido | No | `50` |
| `ARTIKOS_HTTP_CONNECT_TIMEOUT_MS` | Timeout conexión SOAP | No | `5000` |
| `ARTIKOS_HTTP_READ_TIMEOUT_MS` | Timeout lectura SOAP | No | `30000` |

## Oracle schemas and service users

La aplicación usa Oracle para:

- `CONTROL_NOMINA`: control funcional por nómina, mediante el datasource de aplicación.
- `GRL_MAE_ITEM`: lookup ASI para Procurement, mediante el datasource de aplicación.
- `GRL_MAE_ITEM_DET`: lookup ASI para Procurement, mediante el datasource de aplicación.
- `BATCH_*`: metadata técnica Spring Batch, mediante el datasource Batch.

Las tablas `BATCH_*` pueden estar en un esquema separado, por ejemplo `BACHPROCESS`. En ese caso configurar:

```properties
SPRING_BATCH_JDBC_TABLE_PREFIX=BACHPROCESS.BATCH_
```

La aplicación configura `spring.batch.jdbc.initialize-schema=never`; no debe asumirse que las tablas Batch serán creadas automáticamente durante el arranque.

Los usuarios de servicio deben tener permisos suficientes, idealmente mediante roles corporativos:

| Objeto | Permisos requeridos |
| --- | --- |
| `GRL_MAE_ITEM` | `SELECT` |
| `GRL_MAE_ITEM_DET` | `SELECT` |
| `CONTROL_NOMINA` | `SELECT`, `INSERT`, `UPDATE` |
| `BATCH_JOB_INSTANCE` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` según política de purga |
| `BATCH_JOB_EXECUTION` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` según política de purga |
| `BATCH_JOB_EXECUTION_PARAMS` | `SELECT`, `INSERT`, `DELETE` según política de purga |
| `BATCH_JOB_EXECUTION_CONTEXT` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` según política de purga |
| `BATCH_STEP_EXECUTION` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` según política de purga |
| `BATCH_STEP_EXECUTION_CONTEXT` | `SELECT`, `INSERT`, `UPDATE`, `DELETE` según política de purga |

Para el modelo de mantenimiento y purga consultar `docs/technical-maintenance.md`.

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

## Pipeline y despliegue versionado

La `.gitlab-ci.yml` actual declara las etapas:

```text
test
build
deploy
cleanup
```

El pipeline utiliza componentes corporativos para:

- Docker lint;
- Docker build;
- Docker deploy;
- deploy review;
- SonarQube;
- Azure App Configuration;
- Azure Key Vault.

La configuración versionada del componente de deploy indica actualmente:

| Elemento | Valor versionado |
| --- | --- |
| Namespace Kubernetes | `artikos` |
| Recurso Flux | `artikos-integration` |
| Repositorio IaC | `zs/zs-kubernetes/artikos/iac-artikos-integration.git` |

Estos valores describen lo que actualmente está declarado en `.gitlab-ci.yml`. Los detalles internos de los componentes corporativos de CI/CD pertenecen a la plataforma del cliente.

## Dockerfile actual

El `Dockerfile` versionado utiliza un build multi-stage:

- build: Maven 3.8.5 + OpenJDK 17;
- runtime: Amazon Corretto 17 Alpine;
- artefacto: `target/atk-nomina-batch-*.jar` copiado como `app.jar`;
- puerto expuesto: `8080`;
- comando de ejecución: `java -jar app.jar`.

No se debe documentar `settings.xml`, certificados Artifactory u otros mecanismos como parte del Dockerfile mientras no formen parte del archivo versionado actual.

## Información externa al repositorio

El repositorio permite confirmar el namespace, recurso Flux, repo IaC y componentes incluidos en el pipeline. Sin embargo, ciertos detalles operativos siguen dependiendo de la plataforma cliente, por ejemplo:

- reglas internas de promoción implementadas por componentes corporativos;
- nombre/tag efectivo de la imagen desplegada cuando no sea visible desde el repositorio;
- procedimiento exacto de rollback Kubernetes/Flux;
- aprobaciones manuales requeridas por ambiente.

Estos datos deben validarse contra la infraestructura cliente y no deben inventarse en la documentación.

## Validación de un despliegue

La existencia del cambio en Git no demuestra que el ambiente lo esté ejecutando.

Después de un deploy validar como mínimo:

1. pipeline exitoso;
2. deployment ejecutado;
3. pod/aplicación saludable;
4. `GET /actuator/health` OK;
5. versión/commit esperado correlacionado con la imagen/deployment cuando la plataforma lo permita;
6. logs correspondientes a la versión esperada;
7. configuración y dependencias correctas;
8. prueba funcional controlada.

El procedimiento completo de promoción GitHub → GitLab → PRE → PROD se encuentra en `docs/release-and-deployment.md`.
