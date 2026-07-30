# Deuda Tecnica

## Endpoints diagnosticos

Resuelto parcialmente en Sprint 8.2:

- Endpoints Artikos de diagnostico movidos bajo `/api/v1/dev/artikos/...`.
- Endpoint `CONTROL_NOMINA` de prueba movido bajo `/api/v1/dev/control-nomina/test`.
- Endpoints diagnosticos protegidos con `app.diagnostics.enabled=true`.
- Endpoint duplicado de configuracion Artikos unificado en `/api/v1/dev/artikos/config/{profile}`.

Abordado en Sprint 8.8:

- Documentacion operativa inicial creada en `docs/runbook.md`.
- Consultas SQL de soporte creadas en `docs/sql-queries.md`.
- Guia de escenarios de soporte creada en `docs/support-guide.md`.
- README enlaza la documentacion operacional principal.

Abordado en Sprint 8.9:

- Contrato gateway minimo documentado en `docs/gateway-endpoints.md`.
- `POST /api/v1/nominas/batch/start` queda como unico endpoint productivo inicial para CONC/Kong.
- Endpoints operativos `GET /api/v1/nominas/batch/**` y `GET /api/v1/control-nomina/**` quedan condicionados por `app.endpoints.operations.enabled=true`.
- Headers de trazabilidad de gateway se agregan al MDC sin capturar `Authorization`.

Pendiente:

- Eliminar definitivamente endpoints diagnosticos si el proyecto no los requiere.
- El endpoint administrativo de purga metadata ya queda condicionado por `app.admin.enabled=true`, pero falta protegerlo con autenticacion y autorizacion corporativa.
- Definir si la aplicacion necesitara validaciones defensivas adicionales ademas de la autenticacion/autorizacion aplicada por CONC/Kong.
- Definir si `POST /api/v1/nominas/batch/start` debe exigir siempre body con `profile` o mantener compatibilidad de dry-run sin body.

## Namespace Java

Resuelto en Sprint 8.6.1:

- El paquete base fue migrado desde `cl.poc.atkbatch` hacia `cl.atk.nomina.batch`.
- `docs/decisions/ADR-002-package-namespace.md` queda en estado Accepted.
- Nuevas clases deben mantenerse bajo `cl.atk.nomina.batch`.

## Componentes simulados

Revisar y decidir si se eliminan, se mueven a test fixtures o se protegen por perfil:

- `SimulatedNomina`
- `SimulatedDocumentoContable`
- `NominaItemReader`
- `NominaDocumentoItemReader`
- `NominaItemProcessor`
- `NominaDocumentoItemProcessor`
- `NominaResultItemWriter`

Algunos componentes simulados siguen aportando valor para tests unitarios y escenarios locales, pero no deben confundirse con el flujo operacional real.

## Configuracion y secretos

Abordado parcialmente en Sprint 8.5:

- `application-local.properties` contiene configuracion sensible local y no debe versionarse.
- `application-qa.properties` y `application-prod.properties` usan placeholders sin secretos reales.
- `application-local.example.properties` queda como plantilla segura.
- `docs/secrets.md` documenta nombres logicos, propiedades Spring y nombres sugeridos para Azure Key Vault.
- `app.config.validation.strict=true` queda definido para QA/PROD.

Pendiente:

- Implementar integracion directa con Azure Key Vault si la plataforma no inyecta secretos como variables.
- Definir el mecanismo final de Managed Identity, permisos y rotacion de secretos.
- Revisar que logs y endpoints enmascarados nunca impriman tokens completos.
- Evitar incluir valores reales en archivos de ejemplo.

## Integracion Procurement

Abordado en Sprints 9.0 a 9.7:

- Mapper Artikos -> Procurement CMP implementado.
- DTOs Procurement serializan con nombres JSON esperados.
- `HNR` queda fuera de alcance.
- Cliente HTTP Procurement implementado en Sprint 9.1 para `POST /api/v1/document`.
- Timeouts y URL Procurement quedan configurables.
- `statusCode=0` se interpreta como OK y `statusCode!=0` como NOK funcional.
- Integracion al processor batch implementada en Sprint 9.2, controlada por `procurement.integration.enabled`.
- Errores tecnicos/mapping Procurement marcan `CONTROL_NOMINA` como `ERROR` y fallan el job.
- Idempotencia inicial implementada en Sprint 9.3 sin tabla adicional: `CONTROL_NOMINA` controla reproceso por nomina y duplicados Procurement conocidos se tratan como OK idempotente.
- Respuesta real `statusCode=-20` de Procurement formalizada como OK idempotente en Sprint 9.3.1.
- Modo `artikos.source.mode=local-xml` implementado para pruebas end-to-end tecnicas con XML local cuando QA Artikos no tenga nominas disponibles.
- XML Artikos v2 soportado en Sprint 9.5.
- `Tipo_ERP`, `Msg_To` y `USO_IVA` se usan como fuente principal del mapper Procurement.
- `COD_CONTBL`, `COD_TIP_UNID`, `GRL_COD_ITEM`, `COD_TIP_CNTA_ITEMS`, `COD_SISTEM`, `NUM_PERIODO` y `COD_MONEDA` se resuelven desde `GRL_MAE_ITEM_DET` con validacion de maestro `GRL_MAE_ITEM`.
- Cierre funcional Procurement documentado en `docs/procurement-functional-closure.md`.
- Lookup ASI documentado en `docs/asi-lookup.md`.
- Evidencias sanitizadas del flujo local XML creadas en `docs/evidence/`.
- Procedimiento de replay local con XML capturado documentado en `docs/artikos-replay-local.md`.
- Procedimiento de primera ejecucion remota real documentado en `docs/artikos-remote-e2e.md`.

Pendiente:

- Formalizar con el equipo Procurement que `statusCode=-20` corresponde a `DOCUMENT_ALREADY_EXISTS`.
- Idealmente reemplazar la deteccion por texto por un codigo funcional documentado y estable.
- Evaluar endpoint bulk futuro.
- Definir retry Procurement si aplica.
- Validar con Procurement si `CMP_DOCUMT_DET_RUT.CMP_NUM_RUT` y `NUM_RUT` deben seguir usando ambos el RUT proveedor.
- Validar regla `COD_IMPSTO` con negocio.
- Validar comportamiento final si una nomina trae lineas con distintos `COD_CONTBL`.
- Validar `NUM_PERIODO` contra periodo abierto ASI.
- Revisar si `CONTROL_NOMINA` debe agregar empresa/profile en una evolucion futura para evitar ambiguedad por `NUMERO_NOMINA`.
- Definir estados funcionales y reintentos finos para Procurement.
- Validar funcionalmente el mapeo con nominas reales cuando Artikos QA vuelva a entregar documentos procesables.
- Validar `NOMFACTCONFIR` y `NOMFACTRES` reales contra Artikos QA con nominas en estado correcto.
- Validar catalogo formal de errores Procurement.
- Validar con mas nominas reales despues de la primera ejecucion remota.
- Validar operacion productiva detras de CONC/Kong.

## Contrato REST y errores

- Definir estructura corporativa de error REST si existe.
- Evaluar si `NominaResultResponse.nomfactresXml` debe seguir expuesto en endpoint productivo o moverse a diagnostico/auditoria.
- Documentar codigos HTTP esperados por endpoint en un documento funcional si el cliente lo exige.

## Skills y automatizacion

Revisar skills, scripts auxiliares y convenciones de operacion antes de consolidar el repositorio como base productiva.
