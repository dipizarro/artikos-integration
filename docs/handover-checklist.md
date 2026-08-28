# Checklist final de handover — Artikos Integration

## 1. Propósito

Este documento registra la revisión final de continuidad operativa de `artikos-integration` y sirve como criterio de cierre del milestone `Operational Handover v1.0`.

El objetivo no es sustituir la documentación especializada, sino comprobar que un mantenedor que no participó del desarrollo pueda comprender, levantar, configurar, operar, diagnosticar, mantener y desplegar el servicio utilizando el repositorio y los accesos corporativos correspondientes.

## 2. Resultado del handover

El repositorio cuenta con documentación especializada para las áreas principales:

| Necesidad | Referencia |
|---|---|
| Entrada y mapa documental | [`onboarding.md`](onboarding.md) |
| Arquitectura as-built | [`architecture.md`](architecture.md) |
| Flujo Spring Batch | [`batch-flow.md`](batch-flow.md) |
| Ambientes y dependencias | [`environments-and-dependencies.md`](environments-and-dependencies.md) |
| Operación productiva | [`runbook.md`](runbook.md) |
| Troubleshooting | [`support-guide.md`](support-guide.md) |
| Consultas Oracle | [`sql-queries.md`](sql-queries.md) |
| Mantenimiento técnico | [`technical-maintenance.md`](technical-maintenance.md) |
| Release y despliegue | [`release-and-deployment.md`](release-and-deployment.md) |
| Infraestructura | [`infra-delivery.md`](infra-delivery.md) |
| Checklist de release | [`delivery-checklist.md`](delivery-checklist.md) |

## 3. Comprensión del producto

- [x] README explica el propósito del servicio.
- [x] Se identifica a Artikos como origen/retorno de nóminas.
- [x] Se identifica a Procurement como destino del documento contable.
- [x] Se deja explícito que el adapter no inserta directamente documentos contables en ASI.
- [x] Se identifican los perfiles `VIDA` y `GENERALES`.
- [x] Se diferencia estado funcional (`CONTROL_NOMINA`) de metadata técnica (`BATCH_*`).

## 4. Levantamiento y build

- [x] Java 17 documentado.
- [x] Maven documentado.
- [x] `mvn clean verify` documentado para build + tests.
- [x] Build Docker documentado.
- [x] La ejecución local dispone de configuración de ejemplo sin secretos reales.
- [x] Existe modo `local-xml` para pruebas controladas.
- [x] Las pruebas E2E/replay están documentadas separadamente.

## 5. Configuración Oracle

La aplicación utiliza dos datasources distintos.

```text
APP_DATASOURCE_*
    |
    +--> persistencia JPA de aplicación
    +--> CONTROL_NOMINA
    +--> GRL_MAE_ITEM
    +--> GRL_MAE_ITEM_DET

BATCH_DATASOURCE_*
    |
    +--> JobRepository Spring Batch
    +--> metadata BATCH_*
```

Variables actuales:

```text
APP_DATASOURCE_URL
APP_DATASOURCE_USERNAME
APP_DATASOURCE_PASSWORD
APP_DATASOURCE_DRIVER_CLASS_NAME

BATCH_DATASOURCE_URL
BATCH_DATASOURCE_USERNAME
BATCH_DATASOURCE_PASSWORD
BATCH_DATASOURCE_DRIVER_CLASS_NAME

APP_DB_SCHEMA
SPRING_BATCH_JDBC_TABLE_PREFIX
```

Validación:

- [x] README alineado con el modelo de dos datasources.
- [x] `environments-and-dependencies.md` alineado con el modelo de dos datasources.
- [x] `infra-delivery.md` alineado con el modelo de dos datasources.
- [x] `technical-maintenance.md` explica la responsabilidad de cada datasource.
- [x] No se versionan passwords ni connection strings reales.

## 6. Operación

- [x] Endpoint productivo de inicio documentado: `POST /api/v1/nominas/batch/start`.
- [x] Health documentado: `GET /actuator/health`.
- [x] Ejecución asíncrona y `jobExecutionId` documentados.
- [x] Ruta de observabilidad mediante logs documentada.
- [x] Estados Spring Batch documentados.
- [x] Estados `CONTROL_NOMINA` documentados.
- [x] Regla `COMPLETED` técnico puede coexistir con `NOK` funcional documentada.
- [x] Reintentos seguros documentados.
- [x] Se prohíbe modificar `CONTROL_NOMINA` o borrar `BATCH_*` como recuperación improvisada.

## 7. Diagnóstico y soporte

Un nuevo mantenedor dispone de una ruta de diagnóstico basada en:

```text
jobExecutionId
      |
      v
     logs
      |
      v
BATCH_JOB_EXECUTION
      |
      v
CONTROL_NOMINA
      |
      v
operación externa afectada
```

- [x] Casos Artikos documentados.
- [x] Casos Procurement documentados.
- [x] Casos Oracle documentados.
- [x] Casos plataforma/gateway documentados.
- [x] `sql-queries.md` disponible como catálogo de consultas.
- [x] Ownership se expresa por equipo/rol y no por persona.

## 8. Mantenimiento técnico

- [x] Metadata Spring Batch explicada.
- [x] Relación `BATCH_*` vs `CONTROL_NOMINA` explicada.
- [x] Scripts Oracle inventariados.
- [x] Se deja explícito que no existe Flyway/Liquibase como ejecución automática de los scripts.
- [x] Purga administrativa documentada.
- [x] `dryRun=true` como primera operación documentado.
- [x] Monitoreo de crecimiento y estados aparentemente huérfanos documentado.
- [x] Checklist previo a cambios Spring Batch documentado.

## 9. Release y despliegue

- [x] GitHub corporativo documentado como source of truth del producto.
- [x] GitLab cliente documentado como repositorio de entrega/despliegue.
- [x] Transferencia mediante `git format-patch` documentada.
- [x] SharePoint documentado como canal de transferencia.
- [x] Aplicación mediante `git am` documentada.
- [x] Validación PRE documentada.
- [x] Promoción PROD documentada a nivel conceptual sin inventar comandos cliente.
- [x] Tags equivalentes GitHub/GitLab documentados como objetivo.
- [x] Principio `Código mergeado != código desplegado` documentado.
- [x] Evidencia mínima de release documentada.

## 10. Estado del modelo de ramas corporativo

El modelo objetivo es:

```text
produccion
    ^
    |
preproduccion
    ^
    |
feature/* / fix/* / hotfix/* / docs/*
```

Al cierre documental del handover, el bootstrap de `preproduccion` y `produccion` sigue pendiente de reconciliación con la versión que realmente se encuentra en PROD del cliente.

Estado transitorio:

```text
main = baseline corporativo vigente
```

Reglas:

- no crear `produccion` arbitrariamente desde `main`;
- identificar primero tag/commit productivo en GitLab cliente;
- determinar su equivalente en GitHub;
- explicar cualquier drift;
- crear las ramas objetivo desde el baseline reconciliado.

Esta acción es operativa/de gobierno del repositorio y no invalida el cierre documental del handover mientras se mantenga registrada de forma explícita.

## 11. Dependencias externas

- [x] Artikos documentado.
- [x] Procurement documentado.
- [x] Oracle/ASI documentado.
- [x] Azure App Configuration documentado.
- [x] Azure Key Vault documentado.
- [x] Kubernetes/Flux documentado.
- [x] CONC/Kong documentado.
- [x] Observabilidad/logs documentados.
- [x] Se evita hardcodear secretos y parámetros históricos como fuente de verdad productiva.

## 12. Deuda técnica transferida

El cierre del handover no significa que el backlog técnico esté vacío.

### #20 — datasource y table prefix de purga Spring Batch

Estado: **deuda técnica conocida, no bloqueante para handover documental**.

Riesgo documentado: la purga debe validarse contra el datasource/schema/prefix efectivo antes de una ejecución real.

Control existente:

- endpoint admin deshabilitado por defecto;
- `dryRun=true` por defecto;
- `technical-maintenance.md` indica no ejecutar purga real si existe duda de direccionamiento.

### #21 — scripts Oracle CONTROL_NOMINA V001/V002

Estado: **deuda técnica conocida, no bloqueante para handover documental**.

Riesgo documentado: `V001` ya contiene `COD_EMPRES` y `V002` vuelve a agregarla; no se debe asumir una secuencia V001 -> V002 para una instalación nueva.

### #1 — Remediación SonarQube post-producción

Estado: **backlog de evolución/mantenimiento**.

No corresponde a una brecha de documentación del handover. Debe continuar gestionándose como trabajo técnico separado.

## 13. Brechas que sí bloquearían el cierre

El handover no debe cerrarse si aparece alguna de estas situaciones:

- documentación principal indica variables que la aplicación no consume;
- no existe una ruta clara de operación/diagnóstico;
- los secretos están versionados;
- no se conoce cómo correlacionar una ejecución;
- no existe procedimiento de release o evidencia de promoción;
- una dependencia crítica no está identificada;
- una deuda técnica conocida con riesgo operativo se oculta o no tiene Issue.

Las brechas técnicas abiertas #20/#21 no bloquean el cierre documental porque están identificadas, poseen controles de seguridad documentados y permanecen trazadas mediante Issues independientes.

## 14. Prueba de continuidad

El handover se considera documentalmente preparado si un nuevo desarrollador puede responder desde el repositorio:

- [x] ¿Qué hace la aplicación?
- [x] ¿Qué sistemas participan?
- [x] ¿Cómo se compila y prueba?
- [x] ¿Cómo se configura cada datasource?
- [x] ¿Cómo se inicia y sigue una ejecución?
- [x] ¿Cómo se diagnostica una falla?
- [x] ¿Qué diferencia existe entre `BATCH_*` y `CONTROL_NOMINA`?
- [x] ¿Cómo se mantiene la metadata?
- [x] ¿Cómo llega un cambio a PRE/PROD?
- [x] ¿Qué deuda técnica recibe continuidad?

## 15. Criterio de cierre

Una vez mergeado el Pull Request que incorpora este checklist y corrige las inconsistencias documentales detectadas:

1. cerrar el Issue final de handover;
2. marcar el último frente de la épica #8 como completado;
3. mantener #20 y #21 abiertos como deuda técnica conocida;
4. mantener #1 fuera del alcance del handover;
5. cerrar la épica #8 y el milestone `Operational Handover v1.0` cuando el seguimiento de GitHub quede consistente.

El cierre certifica la preparación documental y operativa del repositorio para continuidad; no certifica que todo el backlog técnico futuro esté resuelto.