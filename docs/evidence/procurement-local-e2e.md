# Procurement local E2E evidence

## Datos de validacion

- Fecha de validacion: 2026-06-23
- Ambiente: local / QA controlado
- Fuente: XML local Artikos v2
- Artikos remoto: no usado
- Procurement: endpoint real o mock controlado, sin URL versionada
- Oracle ASI lookup: usado
- Resultado general: flujo ejecutado correctamente

## Escenario

El batch se ejecuta en modo `local-xml` para validar el flujo tecnico completo sin depender de nominas disponibles en Artikos QA.

Configuracion esperada:

```properties
artikos.source.mode=local-xml
artikos.confirm.enabled=false
artikos.result.enabled=false
procurement.client.enabled=true
procurement.integration.enabled=true
```

## Resultado observado esperado

- El XML local se parsea como nomina Artikos.
- El processor omite llamadas reales a `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`.
- El mapper genera request Procurement `CMP`.
- El lookup ASI resuelve item/unidad/contable/tipo cuenta/sistema/periodo/moneda.
- Procurement responde OK o duplicado idempotente.
- El adapter genera `ResultadoNomina`.
- El adapter genera XML `NOMFACTRES`.
- `CONTROL_NOMINA` queda `OK`, `NOK` o `ERROR` segun el resultado.

## Checklist de evidencias

- [x] XML local parseado correctamente.
- [x] Lookup ASI resolvio item/unidad/contable.
- [x] JSON Procurement generado.
- [x] `POST /api/v1/document` invocado.
- [x] `statusCode=0` interpretado OK o `statusCode=-20` interpretado OK idempotente.
- [x] `ResultadoNomina` generado.
- [x] `NOMFACTRES` generado.
- [x] `CONTROL_NOMINA` actualizado.

## Evidencias sanitizadas

- Request Procurement: `docs/evidence/procurement-sample-request-sanitized.json`
- Response OK: `docs/evidence/procurement-sample-response-ok.json`
- Response duplicado: `docs/evidence/procurement-sample-response-duplicate.json`
- NOMFACTRES: `docs/evidence/nomfactres-sample-sanitized.xml`

## Observaciones

- No se versionan tokens, passwords, URLs privadas ni payloads reales completos.
- Los RUT y numeros de documento del ejemplo estan sanitizados.
- La validacion con Artikos QA real sigue pendiente cuando existan nominas en estado correcto.
