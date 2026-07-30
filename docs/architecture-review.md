# Architecture review

Sprint 8.6 revisa la estructura interna del servicio `atk-nomina-batch` antes de agregar nuevas integraciones.

## Skills Reviewed

| Skill | Estado | Aplicacion en esta revision |
| --- | --- | --- |
| `zs-context` | PROVISIONAL | Se usa para diferenciar CONFIRMADO, SUPUESTO y REQUIERE VALIDACION. |
| `zs-java-standards` | PROVISIONAL | Se usa para revisar separacion por capas, naming, logs, excepciones y configuracion externa. |
| `zs-new-service-onboarding` | PROVISIONAL | Se usa para revisar entregables minimos: README, OpenAPI, errores, ambientes y tests. |
| `zs-microservice-template` | PROVISIONAL | Se usa como referencia de estructura por capas, sin tratarla como regla obligatoria. |
| `zs-api-functional-doc` | PROVISIONAL | Se usa como referencia documental para endpoints, errores y brechas. |

No se detectaron skills faltantes entre las solicitadas para el sprint.

## Current Package Structure

| Package | Responsabilidad |
| --- | --- |
| `cl.atk.nomina.batch` | Clase principal Spring Boot. |
| `cl.atk.nomina.batch.api.controller` | Endpoints REST productivos y diagnosticos. |
| `cl.atk.nomina.batch.api.dto` | Requests/responses REST. |
| `cl.atk.nomina.batch.api.mapper` | Mapeo de entidades internas a DTOs. |
| `cl.atk.nomina.batch.batch.config` | Configuracion Spring Batch y datasource local Oracle. |
| `cl.atk.nomina.batch.batch.reader` | Lectura de nominas desde Artikos o fixtures simulados. |
| `cl.atk.nomina.batch.batch.processor` | Procesamiento de nominas/documentos en steps batch. |
| `cl.atk.nomina.batch.batch.writer` | Escritura de resultados y envio `NOMFACTRES`. |
| `cl.atk.nomina.batch.config` | Propiedades y validadores de configuracion. |
| `cl.atk.nomina.batch.domain` | Modelo de dominio interno y entidad `CONTROL_NOMINA`. |
| `cl.atk.nomina.batch.domain.artikos` | Modelos/configuracion especificos de Artikos. |
| `cl.atk.nomina.batch.domain.error` | Tipos de error de integracion. |
| `cl.atk.nomina.batch.repository` | Repositorios JPA. |
| `cl.atk.nomina.batch.service` | Servicios de negocio, control, parser, store y purga. |
| `cl.atk.nomina.batch.service.artikos` | Cliente SOAP, builders, parsers y masking Artikos. |
| `cl.atk.nomina.batch.shared.exception` | Excepciones compartidas. |
| `cl.atk.nomina.batch.shared.logging` | Contexto MDC para trazabilidad. |
| `cl.atk.nomina.batch.shared.util` | Utilidades compartidas. |

## Confirmed Findings

- CONFIRMADO: los controllers productivos se mantienen fuera de `/api/v1/dev`.
- CONFIRMADO: controllers diagnosticos usan rutas `/api/v1/dev/...`.
- CONFIRMADO: controllers diagnosticos usan `@ConditionalOnProperty(name = "app.diagnostics.enabled", havingValue = "true")`.
- CONFIRMADO: `application-local.properties` esta ignorado por Git y no esta versionado.
- CONFIRMADO: `application.properties`, `application-qa.properties` y `application-prod.properties` no contienen secretos reales.
- CONFIRMADO: QA/PROD usan placeholders para DB y tokens Artikos.
- CONFIRMADO: QA/PROD activan `app.config.validation.strict=true`.
- CONFIRMADO: `ArtikosSoapClient` consume SOAP; builders construyen SOAP; parsers parsean SOAP; processor/writer orquestan el flujo batch.
- CONFIRMADO: los tests usan H2, mocks o XML local; no dependen de Oracle real ni de Artikos real.
- CONFIRMADO: tokens se enmascaran en endpoints diagnosticos y logs.

## Applied Refactors

No se aplicaron refactors de codigo en este sprint porque los hallazgos principales implican decisiones de arquitectura o contrato que requieren validacion antes de mover clases o cambiar comportamiento.

Se aplicaron cambios documentales:

- Se creo este reporte de arquitectura.
- Se creo ADR para el namespace base.
- Se actualizo deuda tecnica.
- Se actualizo README con convenciones de arquitectura y paquetes.

## Pending Refactors

| Pendiente | Estado | Razon |
| --- | --- | --- |
| Namespace base `cl.atk.nomina.batch` | CONFIRMADO | Migrado en Sprint 8.6.1 tras validacion corporativa. |
| Eliminar componentes simulados | REQUIERE VALIDACION | Siguen siendo usados por tests y escenarios locales. |
| Endurecer endpoint admin de purga | REQUIERE VALIDACION | Falta definicion de autenticacion/autorizacion corporativa. |
| Eliminar endpoints diagnosticos | REQUIERE VALIDACION | Estan aislados por property, pero produccion puede exigir remocion completa. |
| Integracion directa Azure Key Vault | REQUIERE VALIDACION | Actualmente la estrategia asume inyeccion por runtime/pipeline o Key Vault externo. |
| Definir error response global | REQUIERE VALIDACION | Hoy se evita stacktrace, pero falta confirmar estructura corporativa de error REST. |
| Revisar exposicion de `nomfactresXml` | SUPUESTO | No contiene token, pero puede ser grande o sensible funcionalmente. |

## Class Name Review

### Diagnostic

Estas clases permanecen porque estan bajo `/api/v1/dev` y protegidas por `app.diagnostics.enabled`:

- `ArtikosDiagnosticController`
- `ArtikosConfigController`
- `ControlNominaDiagnosticController`

### Simulated

Estas clases permanecen porque siguen soportando tests y escenarios locales:

- `SimulatedNomina`
- `SimulatedDocumentoContable`
- `NominaItemReader`
- `NominaDocumentoItemReader`
- `NominaItemProcessor`
- `NominaDocumentoItemProcessor`
- `NominaResultItemWriter`

No se detectaron clases `Poc`, `Fake`, `Temp` o `Demo` en `src/main/java`.

## Controller Review

Productivos:

- `NominaBatchController`
- `ControlNominaController`
- `BatchMetadataAdminController`
- `HealthController`
- `/actuator/health`

Diagnosticos:

- `ArtikosDiagnosticController`
- `ArtikosConfigController`
- `ControlNominaDiagnosticController`

Hallazgo:

- SUPUESTO: `POST /api/v1/nominas/batch/start` permite request body ausente para mantener compatibilidad con dry-run local. Si el contrato productivo exige `profile`, debe validarse con arquitectura antes de cambiarlo.

## DTO Review

- CONFIRMADO: requests principales usan `jakarta.validation`.
- CONFIRMADO: responses de status/summary compactan errores y no devuelven stacktrace completo.
- CONFIRMADO: responses diagnosticas de configuracion Artikos devuelven token enmascarado.
- REQUIERE VALIDACION: definir formato corporativo unico para errores REST.
- REQUIERE VALIDACION: decidir si `NominaResultResponse.nomfactresXml` debe seguir expuesto en endpoint productivo o moverse a diagnostico/auditoria.

## Service Review

- CONFIRMADO: `ArtikosSoapClient` concentra consumo HTTP/SOAP.
- CONFIRMADO: builders SOAP no hacen llamadas externas.
- CONFIRMADO: parsers SOAP no ejecutan logica batch.
- CONFIRMADO: `ControlNominaService` encapsula persistencia/control funcional.
- CONFIRMADO: `BatchMetadataPurgeService` usa `JdbcTemplate` para tablas tecnicas `BATCH_*`, separadas de entidades de dominio.

## Configuration Review

- CONFIRMADO: configuracion base no contiene secretos reales.
- CONFIRMADO: `application-local.example.properties` usa `REPLACE_ME`.
- CONFIRMADO: `application-local.properties` esta en `.gitignore`.
- CONFIRMADO: QA/PROD usan variables `${...}` para DB y Artikos.
- CONFIRMADO: diagnostics esta deshabilitado por defecto.

## Logging Review

- CONFIRMADO: no se detecto logging de token completo en INFO.
- CONFIRMADO: XML SOAP completo queda limitado a DEBUG enmascarado.
- CONFIRMADO: logs operativos usan `jobExecutionId`, `profile`, `numeroNomina` y `operation` cuando aplica.

## Exception Review

- CONFIRMADO: existe `ArtikosIntegrationException` con `IntegrationErrorType`.
- CONFIRMADO: errores de parser XML usan `NominaXmlParsingException`.
- CONFIRMADO: SOAP tecnico usa `ArtikosSoapClientException`.
- REQUIERE VALIDACION: definir una respuesta REST global de errores para reemplazar respuestas String puntuales.

## Risks

- CONFIRMADO: package base migrado a `cl.atk.nomina.batch` en Sprint 8.6.1.
- REQUIERE VALIDACION: no hay autenticacion/autorizacion definida para endpoints productivos o admin.
- REQUIERE VALIDACION: endpoint administrativo de purga debe protegerse antes de produccion.
- SUPUESTO: mantener componentes simulados en main es aceptable mientras sigan usados por tests/local; podria requerir moverlos a test fixtures.

## Questions For Architecture

- Namespace Java corporativo definitivo: `cl.atk.nomina.batch`.
- El endpoint batch start debe aceptar body ausente o `profile` debe ser obligatorio siempre?
- Cual es el mecanismo oficial de autenticacion/autorizacion?
- El endpoint de purga metadata debe quedar detras de rol administrativo?
- Se permite exponer XML `NOMFACTRES` por endpoint productivo?
- La integracion Azure Key Vault sera directa desde la app o resuelta por pipeline/runtime?
