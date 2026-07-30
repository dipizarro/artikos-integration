# Release notes

## 0.0.1-SNAPSHOT - Initial infrastructure candidate

### Delivered functionality

- Consulta de nominas Artikos mediante `NOMFACTERP`.
- Confirmacion de recepcion mediante `NOMFACTCONFIR`.
- Procesamiento Spring Batch por nomina.
- Envio de documentos a Procurement CMP.
- Lookup ASI desde `GRL_MAE_ITEM` y `GRL_MAE_ITEM_DET`.
- Generacion de resultado Artikos `NOMFACTRES`.
- Control funcional Oracle en `CONTROL_NOMINA`.
- Metadata tecnica Spring Batch `BATCH_*`.
- Health check Actuator.
- Endpoint productivo minimo para iniciar batch.

### Published endpoints

- `POST /api/v1/nominas/batch/start`
- `GET /actuator/health`

### External dependencies

- Oracle.
- Artikos SOAP QA/PROD.
- Procurement CMP.
- CONC/Kong.
- Azure App Configuration.
- Azure Key Vault.

### Required configuration

Ver [docs/infra-delivery.md](infra-delivery.md).

### Validation

- Flujo local XML validado con Procurement.
- Flujo remoto Artikos preparado para ejecucion con nominas disponibles.
- Errores funcionales y tecnicos documentados en runbook.

### Operational pending items

- Confirmar componentes GitLab corporativos definitivos desde repo modelo `artikos-integration`.
- Confirmar ruta Flux e IaC final.
- Confirmar usuarios de servicio y permisos Oracle en ambiente efimero/preproduccion.
- Confirmar exposicion final por CONC/Kong.
