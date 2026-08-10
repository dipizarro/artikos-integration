# Arquitectura — Estado productivo

## 1. Objetivo

`atk-nomina-batch` es un servicio de integración batch desplegado en producción, responsable de procesar nóminas de documentos contables provenientes de Artikos e integrarlas con Procurement / ASI.

Este documento describe la arquitectura actualmente implementada (`as-built`). Los modos de replay local, diagnóstico, simulación y administración son mecanismos auxiliares de soporte y pruebas; no forman parte del flujo productivo normal.

Para una descripción detallada de clases, servicios, contratos y comportamiento interno consultar [`documentacion-tecnica-artikos-asi.md`](documentacion-tecnica-artikos-asi.md).

## 2. Vista general

La solución actúa como capa de integración entre Artikos, Procurement y ASI.

```text
                   +------------------+
                   |     Artikos      |
                   |                  |
                   | NOMFACTERP       |
                   | NOMFACTCONFIR    |
                   | NOMFACTRES       |
                   +--------+---------+
                            |
                           SOAP
                            |
                            v
               +-------------------------+
               |    atk-nomina-batch     |
               |                         |
               | REST API                |
               | Spring Batch            |
               | Mapping / processing    |
               | Control funcional       |
               +------+------------+-----+
                      |            |
                   REST|            |JDBC
                      |            |
                      v            v
              +-------------+   +---------+
              | Procurement |   | Oracle  |
              +------+------+   +---------+
                     |
                     v
                    ASI
```

### Responsabilidades

**Artikos**

- mantiene las nóminas disponibles para procesamiento;
- entrega una nómina mediante `NOMFACTERP`;
- recibe la confirmación de recepción mediante `NOMFACTCONFIR`;
- recibe el resultado consolidado mediante `NOMFACTRES`.

La especificación de integración Artikos indica que, cuando existen nóminas disponibles, se informa la de fecha de procesamiento más antigua. Luego de recibirla, el consumidor debe confirmar su recepción; si no se recibe una confirmación válida, la nómina puede continuar disponible para una solicitud posterior. El ciclo de consulta debe continuar hasta que Artikos informe que no existen más nóminas para procesar.

**atk-nomina-batch**

- inicia y orquesta el procesamiento;
- consulta y confirma nóminas Artikos;
- interpreta los documentos recibidos;
- realiza homologaciones requeridas por ASI;
- construye el contrato Procurement;
- envía cada documento a Procurement;
- consolida los resultados;
- construye y envía `NOMFACTRES`;
- registra trazabilidad funcional y técnica.

**Procurement**

- recibe el contrato documental enviado por el adapter;
- aplica su propia lógica de procesamiento;
- gestiona la persistencia final del documento en ASI.

El adapter **no inserta directamente documentos contables en las tablas finales de ASI**.

**Oracle / ASI**

El adapter accede directamente a Oracle para:

- `CONTROL_NOMINA`;
- tablas Spring Batch `BATCH_*`;
- `GRL_MAE_ITEM`;
- `GRL_MAE_ITEM_DET`.

## 3. Componentes principales

La aplicación utiliza como paquete base:

```text
cl.atk.nomina.batch
```

La decisión de namespace se encuentra documentada en [`decisions/ADR-002-package-namespace.md`](decisions/ADR-002-package-namespace.md).

### API REST

Expone el contrato utilizado para iniciar el procesamiento batch.

El endpoint productivo principal es:

```http
POST /api/v1/nominas/batch/start
```

Los endpoints operativos, administrativos y diagnósticos adicionales están condicionados por configuración y no forman parte de la exposición productiva normal.

### Spring Batch

Orquesta el procesamiento mediante job, step, reader, processor y writer. La unidad de trabajo es una **nómina Artikos completa**.

### Integración Artikos

Encapsula la construcción de mensajes SOAP, invocaciones HTTP, parsing de respuestas y las operaciones `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`.

### Integración Procurement

Construye el contrato documental esperado por Procurement y realiza un POST por documento.

### Persistencia

La solución mantiene control funcional mediante `CONTROL_NOMINA` y metadata técnica mediante tablas Spring Batch `BATCH_*`. También consulta maestros ASI utilizados durante el mapping hacia Procurement.

### Dominio

Contiene las estructuras utilizadas para representar nóminas, documentos, conciliaciones, distribuciones, resultados y configuración de integración.

## 4. Inicio y ejecución asíncrona

El procesamiento se inicia mediante:

```text
POST /api/v1/nominas/batch/start
            |
            v
     BatchLauncherService
            |
            v
   JobLauncher asíncrono
            |
            v
nominaDocumentosContablesJob
            |
            v
processNominaDocumentosStep
```

El endpoint no espera el término completo del batch. Una vez aceptada la solicitud, la aplicación retorna un `jobExecutionId` y el job continúa ejecutándose en segundo plano.

El `jobExecutionId` es la referencia técnica principal para seguimiento, búsqueda de logs, consulta de metadata Spring Batch y correlación con `CONTROL_NOMINA`.

## 5. Flujo de procesamiento

El job procesa una nómina completa como item de Spring Batch.

```text
Reader
  |
  v
Nómina Artikos
  |
  v
Processor
  |
  +--> CONTROL_NOMINA = PROCESSING
  |
  +--> NOMFACTCONFIR
  |
  +--> parsing / validaciones
  |
  +--> lookup ASI
  |
  +--> mapping Procurement
  |
  +--> POST Procurement por documento
  |
  +--> construcción NOMFACTRES
  |
  v
Writer
  |
  +--> envío NOMFACTRES
  |
  +--> CONTROL_NOMINA
       OK / NOK / ERROR
```

El reader vuelve a consultar Artikos después de cada nómina. La condición funcional normal de término ocurre cuando Artikos informa que no existen más nóminas disponibles.

`maxNominas` constituye únicamente un límite operacional de seguridad.

El modelo funcional oficial de Artikos establece el siguiente ciclo:

```text
NOMFACTERP
    |
    v
Recepción de nómina
    |
    v
NOMFACTCONFIR
    |
    v
Procesamiento interno / Procurement
    |
    v
NOMFACTRES
    |
    v
Actualización de documentos en Artikos
```

En `NOMFACTRES` se informa la cantidad total de documentos procesados, la cantidad OK, la cantidad NOK y el resultado por documento.

## 6. Chunk y unidad de procesamiento

El procesamiento real utiliza una nómina completa como unidad del step.

El tamaño del chunk se controla mediante:

```properties
atk.batch.real.chunk-size
```

Para el procesamiento remoto se utiliza/recomienda valor `1`. Esta configuración es coherente con la naturaleza de la integración, ya que cada nómina implica operaciones externas con efectos funcionales propios: confirmación Artikos, procesamiento de documentos, envío de resultado Artikos y actualización del control funcional.

## 7. Concurrencia

Antes de iniciar una ejecución, la aplicación verifica si existe un job activo para el mismo perfil.

Los estados considerados activos incluyen:

- `STARTING`;
- `STARTED`;
- `STOPPING`.

No se permiten dos ejecuciones simultáneas para el mismo perfil.

Los perfiles `VIDA` y `GENERALES` pueden ejecutarse de forma independiente dentro del límite global de concurrencia configurado para el launcher.

Este control busca evitar que una misma cola funcional sea procesada simultáneamente por dos ejecuciones.

## 8. Persistencia

### 8.1 CONTROL_NOMINA

`CONTROL_NOMINA` representa la trazabilidad funcional de cada nómina.

Estados principales:

- `PROCESSING`;
- `OK`;
- `NOK`;
- `ERROR`.

Permite relacionar una nómina con su `jobExecutionId`, cantidades procesadas, resultado funcional e información de error cuando corresponde.

`CONTROL_NOMINA` no debe confundirse con la metadata interna de Spring Batch.

### 8.2 Metadata Spring Batch

Las tablas `BATCH_*` almacenan la información técnica requerida por Spring Batch.

Entre ellas:

- `BATCH_JOB_INSTANCE`;
- `BATCH_JOB_EXECUTION`;
- `BATCH_JOB_EXECUTION_PARAMS`;
- `BATCH_JOB_EXECUTION_CONTEXT`;
- `BATCH_STEP_EXECUTION`;
- `BATCH_STEP_EXECUTION_CONTEXT`.

Estas tablas permiten conocer ejecuciones, parámetros, estados, steps, tiempos e información técnica de término.

La purga controlada de metadata afecta únicamente las tablas `BATCH_*` y no elimina registros de `CONTROL_NOMINA`.

### 8.3 Lookup ASI

Durante la construcción del contrato Procurement se consulta:

- `GRL_MAE_ITEM`;
- `GRL_MAE_ITEM_DET`.

Estos objetos son utilizados para resolver y validar información requerida por el modelo ASI. El adapter realiza únicamente consultas sobre estos maestros; la persistencia documental final es responsabilidad de Procurement.

## 9. Estados funcionales Artikos

La integración depende del estado funcional de la nómina dentro de Artikos.

Las validaciones identificadas durante la implementación establecen que:

- `NOMFACTCONFIR` requiere una nómina en estado compatible con confirmación;
- `NOMFACTRES` requiere una nómina en estado compatible con recepción del resultado.

Un rechazo funcional Artikos puede llegar mediante un `MsgStatus` distinto de cero.

La especificación Artikos también establece que una nómina recibida pero no confirmada correctamente puede continuar disponible para futuras solicitudes. Por esta razón una nómina no debe reenviarse manualmente sin conocer previamente su estado actual en Artikos y el resultado de la ejecución anterior.

La decisión arquitectónica correspondiente se encuentra documentada en [`decisions/ADR-003-artikos-nomina-state-transitions.md`](decisions/ADR-003-artikos-nomina-state-transitions.md).

## 10. Componentes auxiliares

La aplicación contiene mecanismos adicionales destinados a desarrollo, pruebas y soporte, entre ellos:

- fuente local XML;
- replay de nóminas;
- endpoints operativos;
- endpoints diagnósticos;
- endpoints administrativos;
- componentes de simulación utilizados por pruebas.

Estos componentes no modifican el flujo arquitectónico productivo principal. Su disponibilidad depende de configuración y los endpoints administrativos o diagnósticos deben permanecer deshabilitados por defecto en producción.

Consultar:

- [`local-e2e-testing.md`](local-e2e-testing.md)
- [`artikos-replay-local.md`](artikos-replay-local.md)
- [`gateway-endpoints.md`](gateway-endpoints.md)

## 11. Infraestructura

A nivel de infraestructura, la solución utiliza componentes corporativos para ejecución en contenedores, orquestación Kubernetes, CI/CD, gateway, configuración por ambiente, gestión de secretos y observabilidad.

La arquitectura de aplicación evita almacenar secretos dentro del repositorio.

Los detalles de despliegue, permisos, configuración y exposición se mantienen separados de este documento. Consultar [`infra-delivery.md`](infra-delivery.md).

## 12. Observabilidad y diagnóstico

La trazabilidad de una ejecución se realiza principalmente mediante:

```text
jobExecutionId
profile
numeroNomina
operation
```

El diagnóstico debe correlacionar tres fuentes:

```text
Logs
  +
CONTROL_NOMINA
  +
BATCH_*
```

Esto permite diferenciar el resultado técnico del job, el resultado funcional de la nómina y la integración externa que produjo una falla.

Consultar:

- [`runbook.md`](runbook.md)
- [`support-guide.md`](support-guide.md)
- [`sql-queries.md`](sql-queries.md)

## 13. Referencia funcional Artikos

La implementación se apoya funcionalmente en la documentación externa entregada por Artikos:

- **Especificación de Modelo de Integración con ERP desde Sistema de Administración de Facturas (SAF)**, versión **1.4.1**.

Esta referencia define el modelo de interacción y los contratos funcionales asociados a `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`.

Los archivos de parámetros específicos por ambiente entregados por Artikos no se versionan en este repositorio cuando contienen tokens, credenciales u otros valores sensibles.

## 14. Documentación relacionada

| Tema | Documento |
|---|---|
| Onboarding | [`onboarding.md`](onboarding.md) |
| Implementación técnica detallada | [`documentacion-tecnica-artikos-asi.md`](documentacion-tecnica-artikos-asi.md) |
| Flujo Spring Batch | [`batch-flow.md`](batch-flow.md) |
| Mapping Procurement | [`procurement-mapping.md`](procurement-mapping.md) |
| Lookup ASI | [`asi-lookup.md`](asi-lookup.md) |
| Operación | [`runbook.md`](runbook.md) |
| Troubleshooting | [`support-guide.md`](support-guide.md) |
| SQL soporte | [`sql-queries.md`](sql-queries.md) |
| Infraestructura | [`infra-delivery.md`](infra-delivery.md) |
| Decisiones arquitectónicas | [`decisions/`](decisions/) |

## 15. Principio arquitectónico de continuidad

`architecture.md` describe los límites y responsabilidades actuales de la solución.

Los detalles de implementación deben mantenerse en los documentos especializados y en los ADR correspondientes.

Cuando una modificación cambie responsabilidades entre sistemas, flujo principal, unidad de procesamiento, persistencia, estrategia de concurrencia, contratos de integración o infraestructura relevante, se debe evaluar también la actualización de esta documentación.
