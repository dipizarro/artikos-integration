# Delivery checklist

Este checklist acompaña el ciclo productivo descrito en `docs/release-and-deployment.md`.

## Antes del PR / merge corporativo

### Repository

- [ ] Existe Issue asociado cuando corresponde.
- [ ] El cambio está en una branch específica.
- [ ] No se modificó directamente una rama protegida.
- [ ] Repo limpio antes de merge.
- [ ] `target/` no versionado.
- [ ] `logs/` no versionado.
- [ ] JAR generado no versionado.
- [ ] `application-local.properties` no versionado.
- [ ] `.env` y archivos de secretos no versionados.
- [ ] ZIPs locales de SQL no versionados.
- [ ] No hay passwords, tokens ni connection strings reales en commits.

### Build y pruebas

- [ ] `mvn clean test` OK.
- [ ] `mvn clean package -DskipTests` OK.
- [ ] Se genera `target/atk-nomina-batch-*.jar`.
- [ ] `docker build -t atk-nomina-batch:local .` OK cuando aplica.
- [ ] Pruebas específicas del cambio ejecutadas.
- [ ] Diff del PR revisado.
- [ ] Evidencia funcional adjunta o referenciada.
- [ ] Resultado de Sonar/pipeline revisado cuando aplique.

### Configuration

- [ ] El cambio no expone secretos.
- [ ] La configuración requerida está identificada.
- [ ] QA/PROD usan `artikos.source.mode=remote` cuando corresponde.
- [ ] `app.diagnostics.enabled=false`.
- [ ] `app.admin.enabled=false`.
- [ ] `app.endpoints.operations.enabled=false`.
- [ ] `springdoc.api-docs.enabled=false` en QA/PROD.
- [ ] `springdoc.swagger-ui.enabled=false` en QA/PROD.
- [ ] `PROCUREMENT_INTEGRATION_ENABLED` definido según ambiente.
- [ ] `SPRING_BATCH_JDBC_TABLE_PREFIX` definido si `BATCH_*` vive en schema separado.

## Preparar transferencia GitHub -> GitLab

- [ ] PR corporativo aprobado.
- [ ] Branch origen registrada antes de eliminarla.
- [ ] `BASE_SHA` registrado.
- [ ] `HEAD_SHA` registrado.
- [ ] Lista exacta de commits registrada.
- [ ] El rango contiene solo commits del cambio aprobado.
- [ ] `git format-patch BASE_SHA..HEAD_SHA` generado.
- [ ] Cantidad/orden de patches coincide con los commits esperados.
- [ ] Patches revisados para descartar secretos o archivos ajenos al PR.
- [ ] Manifest de transferencia generado.
- [ ] Issue GitHub y PR GitHub registrados en el manifest.
- [ ] Versión candidata registrada cuando corresponda.
- [ ] Paquete transferido mediante SharePoint.

## Aplicar cambio en GitLab cliente

- [ ] `preproduccion` cliente actualizado antes de crear branch.
- [ ] Branch GitLab equivalente creada desde `preproduccion`.
- [ ] `git am *.patch` ejecutado.
- [ ] Si hubo conflicto, se investigó el drift antes de resolver manualmente.
- [ ] Si el intento fue inválido, se utilizó `git am --abort` antes de reintentar.
- [ ] `git log --oneline` revisado.
- [ ] `git diff preproduccion...HEAD` corresponde al cambio corporativo.
- [ ] Mapeo SHA GitHub -> SHA GitLab registrado cuando sea necesario.
- [ ] MR GitLab creado hacia `preproduccion`.
- [ ] MR incluye Issue, PR y commit origen corporativos.
- [ ] Summary del MR representa el mismo cambio lógico que el PR GitHub.

## Validación PRE cliente

- [ ] MR aprobado y mergeado.
- [ ] Pipeline PRE exitoso.
- [ ] Deployment PRE confirmado.
- [ ] Aplicación/pod saludable.
- [ ] `GET /actuator/health` OK.
- [ ] Versión/commit esperado correlacionado con deployment cuando la plataforma lo permita.
- [ ] Logs corresponden a la versión esperada.
- [ ] Configuración del ambiente validada.
- [ ] Dependencias externas requeridas disponibles.
- [ ] Prueba funcional controlada OK.
- [ ] Resultado PRE registrado en el seguimiento corporativo.

### Si el cambio está mergeado pero no se observa

- [ ] No se asumió inmediatamente un error de código.
- [ ] Commit esperado verificado.
- [ ] Imagen/build verificado.
- [ ] Deploy/pipeline verificado.
- [ ] Pod/versión activa verificada.
- [ ] Logs de versión revisados.

## Producción cliente

- [ ] PRE aprobado funcionalmente.
- [ ] Promoción a PROD realizada mediante el procedimiento cliente.
- [ ] Pipeline/deploy PROD exitoso.
- [ ] Health PROD correcto.
- [ ] Logs corresponden a la versión esperada.
- [ ] Validación funcional/productiva realizada cuando corresponde.
- [ ] Estado de nóminas parcialmente procesadas revisado si el cambio/incidente lo requiere.
- [ ] Tag GitLab `vX.Y.Z` creado/actualizado sobre la versión productiva confirmada.

## Sincronización final en GitHub corporativo

- [ ] Producción cliente confirmada antes de promover GitHub corporativo.
- [ ] PR `preproduccion -> produccion` creado cuando el modelo de ramas ya esté inicializado.
- [ ] PR corporativo revisado y mergeado.
- [ ] Tag GitHub idéntico al tag GitLab creado.
- [ ] Manifest actualizado con PRE, PROD, MR y tag final.
- [ ] Issue actualizado con trazabilidad completa.
- [ ] Issue cerrado solo después de confirmar la entrega productiva cuando su alcance lo requiera.

## Oracle

- [ ] Scripts de metadata/control revisados si el release modifica base de datos.
- [ ] Permisos `CONTROL_NOMINA` validados cuando aplica.
- [ ] Permisos `BATCH_*` validados cuando aplica.
- [ ] Permisos `GRL_MAE_ITEM` validados cuando aplica.
- [ ] Permisos `GRL_MAE_ITEM_DET` validados cuando aplica.

## Gateway

- [ ] Publicado `POST /api/v1/nominas/batch/start` según política vigente.
- [ ] Health interno disponible en `GET /actuator/health`.
- [ ] Endpoints diagnósticos no publicados.
- [ ] Endpoints admin no publicados.
- [ ] Endpoints operativos GET no publicados por defecto.
- [ ] CONC/Kong aplica autenticación, autorización y políticas corporativas.

## External dependencies

- [ ] Artikos `NOMFACTERP` disponible.
- [ ] Artikos `NOMFACTCONFIR` disponible.
- [ ] Artikos `NOMFACTRES` disponible.
- [ ] Procurement `/api/v1/document` disponible.
- [ ] Azure App Configuration disponible/configurado.
- [ ] Azure Key Vault disponible/configurado.

## Rollback / incidente post-deploy

- [ ] Versión previa conocida.
- [ ] Impacto funcional evaluado.
- [ ] Estado Artikos/Procurement revisado si hubo procesamiento parcial.
- [ ] Compatibilidad de configuración/base de datos revisada.
- [ ] No se utilizaron `CONTROL_NOMINA` ni `BATCH_*` como mecanismo improvisado de rollback.
- [ ] Procedimiento de rollback coordinado con la plataforma cliente.
