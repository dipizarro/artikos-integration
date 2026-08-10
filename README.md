# Artikos Integration - ATK Nómina Batch

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring_Batch-brightgreen)](https://spring.io/projects/spring-batch)
[![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-brightgreen)](https://spring.io/projects/spring-data-jpa)
[![Spring Validation](https://img.shields.io/badge/Spring_Validation-brightgreen)](https://docs.spring.io/spring-framework/reference/core/validation.html)
[![Oracle JDBC](https://img.shields.io/badge/Oracle_JDBC-brightgreen)](https://www.oracle.com/database/technologies/appdev/jdbc.html)
[![Java Version](https://img.shields.io/badge/Java-17-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Docker](https://img.shields.io/badge/Docker-enabled-blue)](https://www.docker.com/)

# Tabla de contenidos

* [Descripción](#descripción)
* [Onboarding y continuidad](#onboarding-y-continuidad)
* [Autores](#autores)
* [Tecnologías utilizadas](#tecnologías-utilizadas)
* [API](#api)
* [Prerrequisitos](#prerrequisitos)
* [Configuración](#configuración)
* [Ejecutar la aplicación](#ejecutar-la-aplicación)
* [Documentación operativa](#documentación-operativa)

## Descripción

`atk-nomina-batch` es un microservicio desarrollado con Spring Boot y Spring Batch, encargado de procesar nóminas de documentos contables disponibles en Artikos e integrarlas con Procurement / ASI.

La aplicación ejecuta el siguiente flujo funcional:

```text
Artikos NOMFACTERP -> Adapter atk-nomina-batch -> Procurement /api/v1/document -> Artikos NOMFACTRES
```

El servicio consulta nóminas disponibles en Artikos, confirma la recepción, transforma los documentos contables al contrato requerido por Procurement, envía los documentos uno a uno, genera el XML de resultado para Artikos y registra el control funcional del proceso en Oracle.

El estado funcional de cada nómina se almacena en la tabla `CONTROL_NOMINA`. La metadata técnica de ejecución queda registrada en las tablas Spring Batch `BATCH_*`.

El contrato productivo inicial expone únicamente el endpoint de inicio del batch y el endpoint de health. Los endpoints operativos, diagnósticos y administrativos quedan deshabilitados por defecto en QA/PROD.

## Onboarding y continuidad

Si estás comenzando a mantener este servicio, utiliza [`docs/onboarding.md`](docs/onboarding.md) como punto de entrada. La guía resume el flujo funcional, los componentes críticos, la ruta de lectura recomendada, el primer diagnóstico de soporte y el mapa hacia la documentación especializada del repositorio.

## Autores

<img src="https://soap.zurichsantanderseguros.cl/soap-web/assets/img/Zurich-Santander-Chile.png" alt="Company logo" width="200">

* Este proyecto fue desarrollado para la integración Zurich Santander / Artikos.
* Entrega técnica: ZS Arquitectura TI.
* Aplicación: `atk-nomina-batch`.

## Tecnologías utilizadas

* **Java 17:** Versión de runtime utilizada por la aplicación.
* **Spring Boot:** Framework principal para la construcción del servicio.
* **Spring Batch:** Motor de procesamiento batch para las nóminas Artikos.
* **Spring Data JPA:** Acceso a datos Oracle y lookup ASI.
* **Spring Validation:** Validación de entrada REST y configuración.
* **Oracle JDBC:** Conectividad con base de datos Oracle.
* **Maven:** Gestión de dependencias y ciclo de build.
* **Docker:** Construcción y empaquetado de imagen de contenedor.
* **GitLab CI/CD:** Pipeline corporativo para lint, build, calidad y despliegue.
* **Azure App Configuration:** Configuración no sensible por ambiente.
* **Azure Key Vault:** Gestión de secretos, tokens y passwords.
* **Kubernetes / Flux:** Modelo de despliegue administrado por Infraestructura.
* **CONC/Kong:** API Gateway corporativo para autenticación, autorización y exposición.

## API

### URL base

La URL final depende del ambiente y de la configuración del gateway.

Valor local por defecto:

```text
http://localhost:8080
```

### Endpoints expuestos

La exposición productiva inicial se limita a:

| Método | Endpoint                      | Descripción                                 |
| ------ | ----------------------------- | ------------------------------------------- |
| `POST` | `/api/v1/nominas/batch/start` | Inicia el proceso batch de nóminas Artikos. |
| `GET`  | `/actuator/health`            | Endpoint de health para monitoreo interno.  |

### Ejemplo de inicio de batch

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

Ejemplo con límite operativo:

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

### Endpoints no expuestos por defecto

Los siguientes endpoints no quedan disponibles por defecto en QA/PROD y no deben publicarse inicialmente a través del gateway:

* `/api/v1/dev/**`
* `/api/v1/admin/**`
* `/api/v1/control-nomina/**`
* `/api/v1/nominas/batch/**` para consultas operativas GET
* `/swagger-ui.html`
* `/v3/api-docs`

Ver `docs/gateway-endpoints.md` para la matriz de exposición por gateway.

## Prerrequisitos

* **Java 17**
* **Maven 3.x**
* **Docker**
* **Base de datos Oracle**
* **Acceso a GitLab CI/CD**
* **Azure App Configuration**
* **Azure Key Vault**
* **Acceso de red a servicios SOAP Artikos**
* **Acceso de red a API Procurement**
* **Infraestructura Kubernetes administrada por Infra**

## Configuración

La aplicación se configura mediante variables de entorno, Azure App Configuration y Azure Key Vault.

Los valores no sensibles pueden configurarse en Azure App Configuration. Los secretos, como passwords, credenciales de servicio y tokens Artikos, deben almacenarse en Azure Key Vault o en un mecanismo seguro equivalente.

### Variables de entorno

| Variable                                 | Descripción                                                                                            | Secreto |
| ---------------------------------------- | ------------------------------------------------------------------------------------------------------ | ------- |
| `SPRING_PROFILES_ACTIVE`                 | Perfil Spring activo. Valores esperados: `qa`, `prod` o perfil local.                                  | No      |
| `DB_URL`                                 | URL JDBC Oracle.                                                                                       | Sí      |
| `DB_USERNAME`                            | Usuario de servicio Oracle.                                                                            | Sí      |
| `DB_PASSWORD`                            | Password del usuario de servicio Oracle.                                                               | Sí      |
| `SPRING_BATCH_JDBC_TABLE_PREFIX`         | Prefijo de tablas Spring Batch. Usar prefijo de esquema si las tablas `BATCH_*` viven en otro esquema. | No      |
| `PROCUREMENT_BASE_URL`                   | URL base del servicio Procurement.                                                                     | No      |
| `PROCUREMENT_INTEGRATION_ENABLED`        | Habilita el envío real de documentos a Procurement.                                                    | No      |
| `ARTIKOS_NOMINA_URL`                     | Endpoint SOAP Artikos para `NOMFACTERP`.                                                               | No      |
| `ARTIKOS_CONNECTOR_URL`                  | Endpoint SOAP Artikos para `NOMFACTCONFIR` y `NOMFACTRES`.                                             | No      |
| `ARTIKOS_GENERALES_CONSUMO_TOKEN`        | Token Artikos GENERALES para consumo de nómina.                                                        | Sí      |
| `ARTIKOS_GENERALES_RESPUESTA_TOKEN`      | Token Artikos GENERALES para confirmación.                                                             | Sí      |
| `ARTIKOS_GENERALES_RESULTADO_TOKEN`      | Token Artikos GENERALES para envío de resultado.                                                       | Sí      |
| `ARTIKOS_VIDA_CONSUMO_TOKEN`             | Token Artikos VIDA para consumo de nómina.                                                             | Sí      |
| `ARTIKOS_VIDA_RESPUESTA_TOKEN`           | Token Artikos VIDA para confirmación.                                                                  | Sí      |
| `ARTIKOS_VIDA_RESULTADO_TOKEN`           | Token Artikos VIDA para envío de resultado.                                                            | Sí      |
| `ARTIKOS_GENERALES_MSG_COD_FROM_ADDRESS` | Código emisor Artikos GENERALES.                                                                       | No      |
| `ARTIKOS_GENERALES_MSG_COD_EXTERNO`      | Código externo Artikos GENERALES.                                                                      | No      |
| `ARTIKOS_VIDA_MSG_COD_FROM_ADDRESS`      | Código emisor Artikos VIDA.                                                                            | No      |
| `ARTIKOS_VIDA_MSG_COD_EXTERNO`           | Código externo Artikos VIDA.                                                                           | No      |
| `ATK_BATCH_DEFAULT_MAX_NOMINAS`          | Límite operativo default por ejecución.                                                                | No      |
| `ATK_BATCH_MAX_NOMINAS_PER_RUN`          | Límite máximo permitido por ejecución.                                                                 | No      |
| `ARTIKOS_HTTP_CONNECT_TIMEOUT_MS`        | Timeout de conexión SOAP Artikos.                                                                      | No      |
| `ARTIKOS_HTTP_READ_TIMEOUT_MS`           | Timeout de lectura SOAP Artikos.                                                                       | No      |

### Valores requeridos para ejecución real

Para una ejecución real en QA/PROD, se debe asegurar la siguiente configuración:

```properties
artikos.source.mode=remote
artikos.confirm.enabled=true
artikos.result.enabled=true
procurement.client.enabled=true
procurement.integration.enabled=true
```

Si `PROCUREMENT_INTEGRATION_ENABLED=false`, la aplicación puede iniciar correctamente, pero no enviará documentos a Procurement.

### Prefijo de tablas Spring Batch

Si las tablas de metadata Spring Batch se encuentran en un esquema Oracle separado, configurar:

```properties
SPRING_BATCH_JDBC_TABLE_PREFIX=BACHPROCESS.BATCH_
```

Si las tablas están en el esquema por defecto del usuario de servicio, puede mantenerse el valor:

```properties
SPRING_BATCH_JDBC_TABLE_PREFIX=BATCH_
```

### Permisos Oracle

El usuario de servicio Oracle requiere acceso a los siguientes objetos:

| Objeto             | Permisos requeridos                                            |
| ------------------ | -------------------------------------------------------------- |
| `CONTROL_NOMINA`   | `SELECT`, `INSERT`, `UPDATE`                                   |
| `GRL_MAE_ITEM`     | `SELECT`                                                       |
| `GRL_MAE_ITEM_DET` | `SELECT`                                                       |
| Tablas `BATCH_*`   | Permisos de metadata Spring Batch según política de despliegue |

Ver `docs/infra-delivery.md` para el detalle de configuración y checklist de infraestructura.

## Ejecutar la aplicación

### Build local

```bash
mvn clean verify
```

Empaquetar sin ejecutar tests:

```bash
mvn clean package -DskipTests
```

### Construir imagen Docker

```bash
docker build -t atk-nomina-batch:local .
```

### Ejecutar contenedor Docker

La aplicación requiere variables específicas por ambiente. Ejemplo de estructura:

```bash
docker run --rm --name atk-nomina-batch \
  -e SPRING_PROFILES_ACTIVE=qa \
  -e DB_URL="jdbc:oracle:thin:@//host:1521/service" \
  -e DB_USERNAME="REPLACE_ME" \
  -e DB_PASSWORD="REPLACE_ME" \
  -e PROCUREMENT_BASE_URL="https://procurement.internal" \
  -e PROCUREMENT_INTEGRATION_ENABLED=true \
  -e ARTIKOS_NOMINA_URL="https://artikos.example/Ws_B2BOut/AtkWS_DocExtractorB2B.asmx" \
  -e ARTIKOS_CONNECTOR_URL="https://artikos.example/Ws_B2BIn/AtkWS_DocConnectorB2B.asmx" \
  -p 8080:8080 \
  atk-nomina-batch:local
```

No utilizar secretos reales en consola local, historial de shell ni archivos versionados. En ambientes administrados, los secretos deben inyectarse mediante Azure Key Vault, variables protegidas de GitLab o el mecanismo corporativo de gestión de secretos.

### Health check

```bash
curl http://localhost:8080/actuator/health
```

### Iniciar batch

```bash
curl -X POST http://localhost:8080/api/v1/nominas/batch/start \
  -H "Content-Type: application/json" \
  -d "{\"profile\":\"GENERALES\",\"dryRun\":false}"
```

## Documentación operativa

La documentación complementaria se encuentra en el directorio `docs/`:

| Documento                      | Propósito                                                         |
| ------------------------------ | ----------------------------------------------------------------- |
| `docs/onboarding.md`           | Punto de entrada para nuevos mantenedores y mapa documental.      |
| `docs/infra-delivery.md`       | Requerimientos de infraestructura, variables y permisos.          |
| `docs/delivery-checklist.md`   | Checklist de entrega antes de merge/deploy.                       |
| `docs/runbook.md`              | Procedimientos operativos y troubleshooting.                      |
| `docs/gateway-endpoints.md`    | Matriz de exposición por gateway y reglas de publicación.         |
| `docs/sql-queries.md`          | Consultas Oracle de soporte para Spring Batch y `CONTROL_NOMINA`. |
| `docs/local-e2e-testing.md`    | Modo de ejecución local con XML.                                  |
| `docs/artikos-replay-local.md` | Replay de XML Artikos antes de ejecución remota.                  |
| `docs/artikos-remote-e2e.md`   | Validación end-to-end remota controlada.                          |
| `docs/support-guide.md`        | Clasificación de errores y acciones de soporte.                   |

## Notas

* `maxNominas` es un límite operativo de seguridad, no la condición funcional de término.
* El batch termina naturalmente cuando Artikos informa que no existen más nóminas disponibles.
* Los samples utilizados por tests automatizados deben permanecer bajo `src/test/resources`.
* No se deben empaquetar XML de ejemplo bajo `src/main/resources`.
* QA/PROD deben mantener deshabilitados por defecto los endpoints diagnósticos, administrativos y operativos GET.
