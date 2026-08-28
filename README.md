# Artikos Integration - ATK Nómina Batch

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring_Batch-brightgreen)](https://spring.io/projects/spring-batch)
[![Java Version](https://img.shields.io/badge/Java-17-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Docker](https://img.shields.io/badge/Docker-enabled-blue)](https://www.docker.com/)

## Descripción

`atk-nomina-batch` es un microservicio Spring Boot + Spring Batch que procesa nóminas de documentos contables disponibles en Artikos y las integra con Procurement / ASI.

```text
Artikos NOMFACTERP
        |
        v
atk-nomina-batch
        |
        +--> NOMFACTCONFIR
        |
        +--> Procurement /api/v1/document
        |
        +--> NOMFACTRES
        |
        v
Oracle: CONTROL_NOMINA + BATCH_*
```

El adapter no inserta directamente documentos contables en ASI. Construye el contrato y lo envía a Procurement; la persistencia final del documento en ASI corresponde a Procurement.

El estado funcional por nómina se registra en `CONTROL_NOMINA`. La metadata técnica de ejecución se registra en las tablas Spring Batch `BATCH_*`.

Los perfiles funcionales soportados son `VIDA` y `GENERALES`.

## Onboarding y continuidad

Para comenzar a mantener el servicio, usar [`docs/onboarding.md`](docs/onboarding.md) como punto de entrada.

Para el cierre y alcance del handover consultar [`docs/handover-checklist.md`](docs/handover-checklist.md).

## Tecnologías principales

- Java 17
- Spring Boot
- Spring Batch
- Spring Data JPA
- Oracle JDBC
- Maven
- Docker
- GitLab CI/CD
- Azure App Configuration
- Azure Key Vault
- Kubernetes / Flux
- CONC/Kong

## API productiva

La exposición inicial se limita a:

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/nominas/batch/start` | Inicia el proceso batch. |
| `GET` | `/actuator/health` | Health interno. |

Ejemplo:

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

La respuesta retorna un `jobExecutionId` y el procesamiento continúa de forma asíncrona.

Los endpoints `/api/v1/admin/**`, `/api/v1/dev/**`, `/api/v1/control-nomina/**`, consultas operativas GET y Swagger/OpenAPI no están expuestos por defecto en QA/PROD.

## Prerrequisitos

- Java 17
- Maven 3.x
- Docker cuando corresponda
- Oracle
- acceso a configuración administrada
- conectividad a Artikos y Procurement

## Configuración

La configuración se resuelve mediante variables de entorno, Azure App Configuration y Azure Key Vault.

Los secretos no deben almacenarse en Git.

### Oracle: dos datasources

La aplicación utiliza dos datasources separados:

```text
APP_DATASOURCE_*
    -> persistencia JPA de aplicación
    -> CONTROL_NOMINA
    -> GRL_MAE_ITEM
    -> GRL_MAE_ITEM_DET

BATCH_DATASOURCE_*
    -> JobRepository Spring Batch
    -> BATCH_*
```

Variables reales consumidas por la aplicación:

| Variable | Uso | Secreto |
|---|---|---|
| `APP_DATASOURCE_URL` | JDBC datasource funcional/JPA | Sí |
| `APP_DATASOURCE_USERNAME` | Usuario datasource funcional/JPA | Sí |
| `APP_DATASOURCE_PASSWORD` | Password datasource funcional/JPA | Sí |
| `APP_DATASOURCE_DRIVER_CLASS_NAME` | Driver JDBC app | No |
| `BATCH_DATASOURCE_URL` | JDBC datasource Spring Batch | Sí |
| `BATCH_DATASOURCE_USERNAME` | Usuario datasource Spring Batch | Sí |
| `BATCH_DATASOURCE_PASSWORD` | Password datasource Spring Batch | Sí |
| `BATCH_DATASOURCE_DRIVER_CLASS_NAME` | Driver JDBC Batch | No |
| `APP_DB_SCHEMA` | Schema JPA de aplicación | No |
| `SPRING_BATCH_JDBC_TABLE_PREFIX` | Schema/prefijo de metadata Batch | No |
| `PROCUREMENT_BASE_URL` | URL Procurement | No |
| `PROCUREMENT_INTEGRATION_ENABLED` | Habilita integración real Procurement | No |
| `ARTIKOS_NOMINA_URL` | SOAP extractor `NOMFACTERP` | No |
| `ARTIKOS_CONNECTOR_URL` | SOAP connector `NOMFACTCONFIR/NOMFACTRES` | No |
| `ATK_BATCH_DEFAULT_MAX_NOMINAS` | Límite operativo por defecto | No |
| `ATK_BATCH_MAX_NOMINAS_PER_RUN` | Límite máximo permitido | No |
| `ARTIKOS_HTTP_CONNECT_TIMEOUT_MS` | Timeout conexión Artikos | No |
| `ARTIKOS_HTTP_READ_TIMEOUT_MS` | Timeout lectura Artikos | No |

Los tokens Artikos por perfil son secretos y deben provenir del mecanismo seguro del ambiente.

Para configuración completa consultar [`docs/environments-and-dependencies.md`](docs/environments-and-dependencies.md).

### Ejecución real

Para una ejecución real deben validarse, según ambiente:

```properties
artikos.source.mode=remote
artikos.confirm.enabled=true
artikos.result.enabled=true
procurement.client.enabled=true
procurement.integration.enabled=true
```

El prefijo Batch puede apuntar a un schema separado, por ejemplo:

```properties
SPRING_BATCH_JDBC_TABLE_PREFIX=BACHPROCESS.BATCH_
```

## Build local

```bash
mvn clean verify
```

Empaquetado:

```bash
mvn clean package -DskipTests
```

Docker:

```bash
docker build -t atk-nomina-batch:local .
```

Para ejecución local se recomienda partir desde `src/main/resources/application-local.example.properties` y usar valores de desarrollo autorizados. No copiar credenciales productivas a archivos locales.

## Permisos Oracle conceptuales

| Objeto | Uso |
|---|---|
| `CONTROL_NOMINA` | `SELECT`, `INSERT`, `UPDATE` |
| `GRL_MAE_ITEM` | `SELECT` |
| `GRL_MAE_ITEM_DET` | `SELECT` |
| `BATCH_*` | lectura/escritura de metadata; `DELETE` solo según política de purga |

Ver [`docs/infra-delivery.md`](docs/infra-delivery.md) y [`docs/technical-maintenance.md`](docs/technical-maintenance.md).

## Documentación

| Documento | Propósito |
|---|---|
| `docs/onboarding.md` | Punto de entrada y mapa documental. |
| `docs/architecture.md` | Arquitectura as-built. |
| `docs/batch-flow.md` | Flujo Spring Batch. |
| `docs/environments-and-dependencies.md` | Ambientes, configuración y ownership. |
| `docs/runbook.md` | Operación productiva. |
| `docs/support-guide.md` | Troubleshooting. |
| `docs/sql-queries.md` | Consultas Oracle de soporte. |
| `docs/technical-maintenance.md` | Mantenimiento Spring Batch/Oracle/metadata. |
| `docs/release-and-deployment.md` | GitHub -> GitLab -> PRE -> PROD. |
| `docs/delivery-checklist.md` | Checklist de entrega y despliegue. |
| `docs/infra-delivery.md` | Infraestructura y configuración de despliegue. |
| `docs/gateway-endpoints.md` | Exposición de endpoints. |
| `docs/local-e2e-testing.md` | E2E local. |
| `docs/artikos-replay-local.md` | Replay de XML. |
| `docs/artikos-remote-e2e.md` | Validación remota controlada. |
| `docs/handover-checklist.md` | Verificación final de continuidad. |

## Notas operativas

- `maxNominas` es un límite de seguridad, no la condición funcional normal de término.
- El batch termina naturalmente cuando Artikos indica que no existen más nóminas.
- No borrar `BATCH_*` ni modificar `CONTROL_NOMINA` para forzar reintentos.
- No versionar secretos, XML productivos sensibles ni archivos locales de credenciales.
- QA/PROD deben mantener deshabilitados por defecto endpoints diagnósticos, administrativos y operativos GET.