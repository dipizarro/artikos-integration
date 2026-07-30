# Gateway endpoint exposure

La aplicacion `atk-nomina-batch` opera detras del API Gateway corporativo CONC/Kong. En este sprint la autenticacion y autorizacion quedan bajo responsabilidad del gateway. La aplicacion no implementa Spring Security, OAuth2, Basic Auth ni JWT.

## Matriz de publicacion

| Endpoint | Disponible en QA/PROD por defecto | Publicar en CONC/Kong | Observacion |
| --- | --- | --- | --- |
| `POST /api/v1/nominas/batch/start` | Si | Si | Contrato productivo minimo. Dispara el proceso batch desde sistema externo. |
| `GET /api/v1/nominas/batch/**` | No | No inicialmente | Consultas operativas internas de status, summary y resultado por nomina. Requieren `app.endpoints.operations.enabled=true`. |
| `GET /api/v1/control-nomina/**` | No | No inicialmente | Consulta interna de control funcional. Requiere `app.endpoints.operations.enabled=true`. |
| `POST /api/v1/admin/batch-metadata/purge` | No | No | Endpoint administrativo. Requiere `app.admin.enabled=true` y debe protegerse con autorizacion corporativa antes de uso productivo. |
| `/api/v1/dev/**` | No | No | Endpoints diagnosticos. Requieren `app.diagnostics.enabled=true`; no deben publicarse en gateway. |
| `GET /actuator/health` | Segun ambiente | Solo monitoreo interno | Debe exponerse solo al mecanismo de monitoreo autorizado por infraestructura. |
| `GET /api/v1/health` | No por defecto | No | Health propio simple. Para entrega inicial preferir `GET /actuator/health`. |
| `GET /swagger-ui.html` | No | No | Deshabilitado en QA/PROD con `springdoc.swagger-ui.enabled=false`. |
| `GET /v3/api-docs` | No | No | Deshabilitado en QA/PROD con `springdoc.api-docs.enabled=false`. |

## Properties de exposicion

Configuracion base para QA/PROD:

```properties
app.endpoints.operations.enabled=false
app.diagnostics.enabled=false
app.admin.enabled=false
```

Configuracion local controlada:

```properties
app.endpoints.operations.enabled=true
app.diagnostics.enabled=true
app.admin.enabled=true
```

## Contrato inicial publicado

El unico endpoint de negocio que debe publicarse inicialmente por CONC/Kong es:

```http
POST /api/v1/nominas/batch/start
```

Ejemplo:

```json
{
  "profile": "GENERALES",
  "dryRun": false
}
```

El gateway debe aplicar los controles corporativos de autenticacion, autorizacion, auditoria, rate limits y politicas de red que correspondan.

## Trazabilidad desde gateway

La aplicacion captura headers enviados por gateway cuando existen y los agrega al MDC de logs:

- `X-Correlation-Id` -> `correlationId`
- `X-Request-Id` -> `requestId`
- `X-Client-Id` -> `clientId`
- `X-Consumer-Username` -> `consumer`
- `X-Forwarded-For` -> `forwardedFor`

La ausencia de estos headers no bloquea el request. La aplicacion no captura ni registra `Authorization`.
