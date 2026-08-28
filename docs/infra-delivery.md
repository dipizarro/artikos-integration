# Infrastructure delivery

Este documento resume la configuración requerida para desplegar `atk-nomina-batch` en la infraestructura del cliente. El ciclo de promoción entre GitHub corporativo y GitLab cliente se documenta en [`release-and-deployment.md`](release-and-deployment.md).

## Configuration source

La aplicación espera configuración por variables de entorno, Azure App Configuration y Azure Key Vault.

- Los valores no sensibles pueden vivir en Azure App Configuration.
- Passwords, tokens Artikos y credenciales de base de datos deben vivir en Azure Key Vault.
- No se deben versionar secretos ni `application-local.properties`.
- QA/PROD deben ejecutar `artikos.source.mode=remote` cuando corresponda a operación real.

Para separación de ambientes, secretos y ownership consultar [`environments-and-dependencies.md`](environments-and-dependencies.md).

## Oracle datasources

La aplicación utiliza dos datasources independientes.

```text
APP_DATASOURCE_*
    -> persistencia JPA de aplicación
    -> CONTROL_NOMINA
    -> GRL_MAE_ITEM
    -> GRL_MAE_ITEM_DET

BATCH_DATASOURCE_*
    -> Spring Batch JobRepository
    -> metadata BATCH_*
```

Las variables actuales son:

| Variable | Descripción | Secreto |
|---|---|---|
| `APP_DATASOURCE_URL` | JDBC URL datasource funcional/JPA | Sí |
| `APP_DATASOURCE_USERNAME` | Usuario datasource funcional/JPA | Sí |
| `APP_DATASOURCE_PASSWORD` | Password datasource funcional/JPA | Sí |
| `APP_DATASOURCE_DRIVER_CLASS_NAME` | Driver JDBC app | No |
| `BATCH_DATASOURCE_URL` | JDBC URL datasource Spring Batch | Sí |
| `BATCH_DATASOURCE_USERNAME` | Usuario datasource Spring Batch | Sí |
| `BATCH_DATASOURCE_PASSWORD` | Password datasource Spring Batch | Sí |
| `BATCH_DATASOURCE_DRIVER_CLASS_NAME` | Driver JDBC Batch | No |
| `APP_DB_SCHEMA` | Schema JPA de aplicación | No |
| `SPRING_BATCH_JDBC_TABLE_PREFIX` | Schema/prefijo de tablas Spring Batch | No |

La aplicación configura `spring.batch.jdbc.initialize-schema=never`; por lo tanto no debe asumirse que crea automáticamente las tablas Batch al iniciar.

Si `BATCH_*` reside en un schema separado, por ejemplo `BACHPROCESS`, configurar:

```properties
SPRING_BATCH_JDBC_TABLE_PREFIX=BACHPROCESS.BATCH_
```

## Otras variables requeridas

| Variable | Descripción | Secreto |
|---|---|---|
| `PROCUREMENT_BASE_URL` | URL base Procurement | No |
| `PROCUREMENT_INTEGRATION_ENABLED` | Habilita envío real a Procurement | No |
| `ARTIKOS_NOMINA_URL` | Endpoint extractor `NOMFACTERP` | No |
| `ARTIKOS_CONNECTOR_URL` | Endpoint connector `NOMFACTCONFIR/NOMFACTRES` | No |
| tokens Artikos VIDA/GENERALES | Tokens por operación/perfil | Sí |
| `ATK_BATCH_DEFAULT_MAX_NOMINAS` | Límite operativo por defecto | No |
| `ATK_BATCH_MAX_NOMINAS_PER_RUN` | Límite máximo por ejecución | No |
| `ARTIKOS_HTTP_CONNECT_TIMEOUT_MS` | Timeout de conexión Artikos | No |
| `ARTIKOS_HTTP_READ_TIMEOUT_MS` | Timeout de lectura Artikos | No |

## Oracle schemas and permissions

La aplicación usa Oracle para:

- `CONTROL_NOMINA`: control funcional por nómina.
- `GRL_MAE_ITEM`: lookup ASI.
- `GRL_MAE_ITEM_DET`: lookup ASI.
- `BATCH_*`: metadata técnica Spring Batch.

Permisos conceptuales:

| Objeto | Permisos requeridos |
|---|---|
| `GRL_MAE_ITEM` | `SELECT` |
| `GRL_MAE_ITEM_DET` | `SELECT` |
| `CONTROL_NOMINA` | `SELECT`, `INSERT`, `UPDATE` |
| `BATCH_JOB_INSTANCE` | `SELECT`, `INSERT`, `UPDATE`; `DELETE` según política de purga |
| `BATCH_JOB_EXECUTION` | `SELECT`, `INSERT`, `UPDATE`; `DELETE` según política de purga |
| `BATCH_JOB_EXECUTION_PARAMS` | `SELECT`, `INSERT`; `DELETE` según política de purga |
| `BATCH_JOB_EXECUTION_CONTEXT` | `SELECT`, `INSERT`, `UPDATE`; `DELETE` según política de purga |
| `BATCH_STEP_EXECUTION` | `SELECT`, `INSERT`, `UPDATE`; `DELETE` según política de purga |
| `BATCH_STEP_EXECUTION_CONTEXT` | `SELECT`, `INSERT`, `UPDATE`; `DELETE` según política de purga |

La política de mantenimiento y purga se documenta en [`technical-maintenance.md`](technical-maintenance.md).

## Endpoint exposure

Publicar inicialmente solo:

- `POST /api/v1/nominas/batch/start`
- `GET /actuator/health` para monitoreo interno

No publicar por defecto:

- `/api/v1/dev/**`
- `/api/v1/admin/**`
- `/api/v1/control-nomina/**`
- consultas operativas GET de `/api/v1/nominas/batch/**`
- Swagger/OpenAPI

## Pipeline y despliegue versionado

La `.gitlab-ci.yml` actual declara las etapas:

```text
test
build
deploy
cleanup
```

Incluye componentes corporativos para:

- Docker lint;
- Docker build;
- Docker deploy;
- deploy review;
- SonarQube;
- Azure App Configuration;
- Azure Key Vault.

La configuración versionada del componente de deploy indica actualmente:

| Elemento | Valor versionado |
|---|---|
| Namespace Kubernetes | `artikos` |
| Recurso Flux | `artikos-integration` |
| Repositorio IaC | `zs/zs-kubernetes/artikos/iac-artikos-integration.git` |

Los detalles internos de los componentes corporativos pertenecen a la plataforma del cliente y no deben inventarse en este repositorio.

## Dockerfile actual

El `Dockerfile` versionado utiliza build multi-stage:

- build: Maven 3.8.5 + OpenJDK 17;
- runtime: Amazon Corretto 17 Alpine;
- artefacto: `target/atk-nomina-batch-*.jar` copiado como `app.jar`;
- puerto: `8080`;
- ejecución: `java -jar app.jar`.

No documentar `settings.xml`, certificados Artifactory u otros mecanismos como parte del Dockerfile mientras no formen parte del archivo versionado.

## Información externa al repositorio

Ciertos detalles siguen dependiendo de la plataforma cliente:

- reglas internas de promoción de componentes corporativos;
- nombre/tag efectivo de la imagen cuando no sea visible desde el repo;
- procedimiento exacto de rollback Kubernetes/Flux;
- aprobaciones manuales por ambiente.

Estos datos deben validarse contra la infraestructura cliente.

## Validación de un despliegue

La existencia del cambio en Git no demuestra que el ambiente lo esté ejecutando.

Después de un deploy validar como mínimo:

1. pipeline exitoso;
2. deployment ejecutado;
3. pod/aplicación saludable;
4. `GET /actuator/health` OK;
5. versión/commit correlacionado con deployment cuando la plataforma lo permita;
6. logs de la versión esperada;
7. configuración y dependencias correctas;
8. prueba funcional controlada.

El procedimiento completo está en [`release-and-deployment.md`](release-and-deployment.md).