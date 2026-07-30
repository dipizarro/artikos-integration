# Delivery checklist

## Repository

- [ ] Repo limpio antes de merge.
- [ ] `target/` no versionado.
- [ ] `logs/` no versionado.
- [ ] JAR generado no versionado.
- [ ] `application-local.properties` no versionado.
- [ ] `.env` y archivos de secretos no versionados.
- [ ] ZIPs locales de SQL no versionados.
- [ ] No hay passwords, tokens ni connection strings reales en commits.

## Build

- [ ] `mvn clean test` OK.
- [ ] `mvn clean package -DskipTests` OK.
- [ ] Se genera `target/atk-nomina-batch-*.jar`.
- [ ] `docker build -t atk-nomina-batch:local .` OK.
- [ ] Variables Artifactory disponibles en GitLab si el build usa repositorio corporativo.
- [ ] Certificado Artifactory disponible en `ZSARTIFACTORY_CERT` si la infra lo exige.

## Configuration

- [ ] QA/PROD usan `artikos.source.mode=remote`.
- [ ] `app.diagnostics.enabled=false`.
- [ ] `app.admin.enabled=false`.
- [ ] `app.endpoints.operations.enabled=false`.
- [ ] `springdoc.api-docs.enabled=false` en QA/PROD.
- [ ] `springdoc.swagger-ui.enabled=false` en QA/PROD.
- [ ] `PROCUREMENT_INTEGRATION_ENABLED` definido segun ambiente.
- [ ] `SPRING_BATCH_JDBC_TABLE_PREFIX` definido si `BATCH_*` vive en schema separado.

## Oracle

- [ ] Scripts `V000__create_spring_batch_metadata.sql` y `V001__create_control_nomina.sql` revisados.
- [ ] Usuario de servicio creado.
- [ ] Permisos `CONTROL_NOMINA` validados.
- [ ] Permisos `BATCH_*` validados.
- [ ] Permisos `GRL_MAE_ITEM` validados.
- [ ] Permisos `GRL_MAE_ITEM_DET` validados.

## Gateway

- [ ] Publicado `POST /api/v1/nominas/batch/start`.
- [ ] Health interno disponible en `GET /actuator/health`.
- [ ] Endpoints diagnosticos no publicados.
- [ ] Endpoints admin no publicados.
- [ ] Endpoints operativos GET no publicados por defecto.
- [ ] CONC/Kong aplica autenticacion, autorizacion y politicas corporativas.

## External dependencies

- [ ] Artikos `NOMFACTERP` disponible.
- [ ] Artikos `NOMFACTCONFIR` disponible.
- [ ] Artikos `NOMFACTRES` disponible.
- [ ] Procurement `/api/v1/document` disponible.
- [ ] Azure App Configuration configurado.
- [ ] Azure Key Vault configurado.
