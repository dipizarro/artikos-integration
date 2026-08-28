# Mantenimiento técnico — Artikos Integration

## 1. Propósito y alcance

Este documento es la referencia para el mantenimiento técnico de `atk-nomina-batch`, con foco en Spring Batch, Oracle, `CONTROL_NOMINA`, metadata `BATCH_*`, scripts de base de datos y operaciones administrativas asociadas.

No reemplaza los documentos operativos existentes:

- operación productiva: [`runbook.md`](runbook.md);
- diagnóstico de incidentes: [`support-guide.md`](support-guide.md);
- consultas Oracle: [`sql-queries.md`](sql-queries.md);
- flujo del job: [`batch-flow.md`](batch-flow.md);
- ambientes, configuración y dependencias: [`environments-and-dependencies.md`](environments-and-dependencies.md).

La finalidad de esta guía es explicar qué estado persiste la aplicación, qué estado pertenece a Spring Batch, cómo se relacionan ambas capas y qué controles deben aplicarse antes de modificar o depurar esa persistencia.

## 2. Mapa del estado persistido

La aplicación mantiene dos capas de estado con responsabilidades diferentes:

```text
                     Oracle
                       |
           +-----------+-----------+
           |                       |
           v                       v
    CONTROL_NOMINA                BATCH_*
   estado funcional       metadata técnica Batch
           |                       |
           |                       +--> JobInstance
           |                       +--> JobExecution
           |                       +--> StepExecution
           |                       +--> parámetros
           |                       +--> execution context
           |
           +--> número de nómina
           +--> estado funcional
           +--> totales
           +--> error asociado
```

Principio central:

```text
BATCH_*         = estado técnico de Spring Batch
CONTROL_NOMINA  = trazabilidad funcional por nómina
```

Estas capas se correlacionan principalmente mediante `JOB_EXECUTION_ID`, pero no son intercambiables.

## 3. Spring Batch y su metadata

Spring Batch utiliza metadata persistida para representar la identidad y las ejecuciones de los jobs y steps.

Conceptualmente:

```text
JobInstance
    |
    +--> identidad lógica del job
             |
             v
       JobExecution
             |
             +--> ejecución concreta
             |
             v
       StepExecution
```

Las principales tablas versionadas por el proyecto son:

- `BATCH_JOB_INSTANCE`;
- `BATCH_JOB_EXECUTION`;
- `BATCH_JOB_EXECUTION_PARAMS`;
- `BATCH_JOB_EXECUTION_CONTEXT`;
- `BATCH_STEP_EXECUTION`;
- `BATCH_STEP_EXECUTION_CONTEXT`.

La metadata permite, entre otras cosas:

- identificar instancias y ejecuciones;
- registrar parámetros del job;
- persistir estados técnicos;
- registrar tiempos y códigos de salida;
- persistir execution contexts;
- soportar el modelo de restartabilidad de Spring Batch.

No se debe eliminar metadata `BATCH_*` manualmente para forzar un reintento o destrabar una nómina. La eliminación puede romper la trazabilidad técnica y no revierte ningún efecto funcional ya ocurrido.

Los estados relevantes incluyen, según el ciclo de vida de Spring Batch:

- `STARTING`;
- `STARTED`;
- `COMPLETED`;
- `FAILED`;
- `STOPPING`;
- `STOPPED`;
- `ABANDONED` cuando corresponda a metadata histórica/administrativa.

Un estado Spring Batch describe el resultado técnico de la ejecución, no el resultado funcional de cada documento o nómina.

## 4. CONTROL_NOMINA

`CONTROL_NOMINA` es una tabla funcional propia de `artikos-integration`.

Su clave primaria está compuesta por:

```text
JOB_EXECUTION_ID + NUMERO_NOMINA
```

Los estados soportados son:

- `PROCESSING`;
- `OK`;
- `NOK`;
- `ERROR`.

Interpretación general:

- `PROCESSING`: la nómina fue tomada para procesamiento;
- `OK`: la nómina terminó funcionalmente correcta;
- `NOK`: la nómina terminó con documentos rechazados u observados;
- `ERROR`: la nómina no pudo cerrarse correctamente por una falla técnica o rechazo que interrumpió el flujo.

`CONTROL_NOMINA` también mantiene totales, timestamps, código de empresa y mensaje de error cuando aplica.

No debe modificarse manualmente como procedimiento normal de recuperación. Un cambio manual puede ocultar la realidad de la ejecución sin corregir su estado técnico ni los efectos realizados en sistemas externos.

## 5. Relación entre BATCH_* y CONTROL_NOMINA

Las dos capas deben analizarse juntas cuando se investiga una ejecución, pero cada una responde una pregunta diferente:

| Capa | Pregunta principal |
|---|---|
| `BATCH_*` | ¿Cómo terminó técnicamente el job/step? |
| `CONTROL_NOMINA` | ¿Cómo terminó funcionalmente cada nómina? |

Por ello es válido encontrar, por ejemplo:

```text
BATCH_JOB_EXECUTION.STATUS = COMPLETED
CONTROL_NOMINA.STATUS      = NOK
```

Esto no representa una contradicción. El job puede haber terminado técnicamente de forma normal y haber procesado documentos con resultado funcional NOK.

Reglas críticas:

```text
Eliminar BATCH_* no cambia CONTROL_NOMINA.
Modificar CONTROL_NOMINA no cambia el estado Spring Batch.
Ninguna de las dos acciones revierte Artikos.
Ninguna de las dos acciones revierte efectos ya realizados en Procurement/ASI.
```

Antes de cualquier reintento operativo consultar [`runbook.md`](runbook.md) y [`support-guide.md`](support-guide.md).

## 6. Datasources y schemas Oracle

La aplicación separa el datasource funcional del datasource de Spring Batch.

### 6.1 Datasource de aplicación

Configuración:

```properties
app.datasource.*
```

Responsabilidades principales:

- persistencia JPA de aplicación;
- `CONTROL_NOMINA`;
- lookups ASI como `GRL_MAE_ITEM` y `GRL_MAE_ITEM_DET`.

`appDataSource` es el datasource primario de la aplicación y utiliza `appTransactionManager` para la persistencia JPA.

El schema JPA de aplicación se configura mediante:

```properties
spring.jpa.properties.hibernate.default_schema=${APP_DB_SCHEMA:ASI}
```

### 6.2 Datasource Spring Batch

Configuración:

```properties
batch.datasource.*
```

Responsabilidad:

- `JobRepository` y metadata técnica Spring Batch.

El proyecto configura un datasource específico anotado como `@BatchDataSource` y un transaction manager batch independiente.

La ubicación/prefijo de las tablas Spring Batch se configura mediante:

```properties
spring.batch.jdbc.table-prefix=${SPRING_BATCH_JDBC_TABLE_PREFIX:BACHPROCESS.BATCH_}
```

Además:

```properties
spring.batch.jdbc.initialize-schema=${SPRING_BATCH_JDBC_INITIALIZE_SCHEMA:never}
```

Por lo tanto, la aplicación no debe asumirse como responsable de crear automáticamente las tablas Spring Batch al iniciar.

Los valores reales por ambiente deben consultarse en [`environments-and-dependencies.md`](environments-and-dependencies.md). No versionar credenciales ni connection strings sensibles.

## 7. Inventario de scripts Oracle

Los scripts actualmente versionados se encuentran bajo:

```text
src/main/resources/db/oracle/
```

Inventario principal:

```text
V000__create_spring_batch_metadata.sql
V001__create_control_nomina.sql
V002__add_cod_empres_to_control_nomina.sql
rollback/V002__drop_cod_empres_from_control_nomina.sql
```

### V000

Crea las tablas y secuencias requeridas por Spring Batch para Oracle. El archivo declara como fuente el schema Oracle de Spring Batch 5.1.2.

### V001

Crea `CONTROL_NOMINA`, índices, constraint de estados y comentarios de tabla/columnas.

### V002

Representa históricamente la incorporación de `COD_EMPRES` a `CONTROL_NOMINA`.

Actualmente existe una inconsistencia conocida: `V001__create_control_nomina.sql` ya contiene `COD_EMPRES`, mientras `V002__add_cod_empres_to_control_nomina.sql` vuelve a agregar la misma columna.

Hasta resolver el Issue #21, no se debe asumir que `V001 -> V002` es una secuencia válida para una instalación nueva.

## 8. Gestión de cambios de base de datos

El proyecto no evidencia actualmente Flyway ni Liquibase como mecanismo automático de migraciones.

Los nombres `V000`, `V001`, `V002` deben entenderse como scripts Oracle versionados, no como prueba de que exista un motor de migraciones ejecutándolos automáticamente.

Principio:

```text
Script versionado en Git != script ejecutado en un ambiente
```

Para un cambio de base de datos se debe conservar como mínimo esta trazabilidad:

```text
Issue
  |
  v
script SQL versionado
  |
  v
revisión SQL
  |
  v
Pull Request
  |
  v
aplicación controlada por ambiente
  |
  v
evidencia
```

Buenas prácticas:

- no modificar estructuras productivas sin conservar el DDL correspondiente en el repositorio;
- identificar si un script es baseline, incremental o rollback;
- validar compatibilidad con el estado real de cada ambiente;
- no asumir que un script fue aplicado solo porque está mergeado;
- documentar ejecución y resultado;
- no incorporar frameworks de migración dentro de una corrección puntual sin decisión arquitectónica explícita.

El procedimiento de promoción entre repositorios y ambientes está documentado en [`release-and-deployment.md`](release-and-deployment.md).

## 9. Purga administrativa de metadata Spring Batch

La metadata `BATCH_*` crece a medida que se ejecutan jobs y eventualmente puede requerir una política de retención.

Existe una operación administrativa específica:

```http
POST /api/v1/admin/batch-metadata/purge
```

El controlador solo se habilita cuando:

```properties
app.admin.enabled=true
```

La purga es una operación de mantenimiento administrativo. No debe utilizarse como mecanismo de recuperación de una nómina ni para forzar un reintento.

### 9.1 Parámetros

`retentionDays`
: cantidad mínima de días de antigüedad; debe ser al menos 1.

`dryRun`
: si se omite, se resuelve como `true`. Permite calcular candidatos sin eliminar registros.

`includeFailed`
: si se omite, se resuelve como `false`.

### 9.2 Candidatos

Por defecto se consideran ejecuciones antiguas con estado:

- `COMPLETED`;
- `ABANDONED`.

Las ejecuciones `FAILED` solo se consideran cuando:

```json
"includeFailed": true
```

La lógica exige `END_TIME IS NOT NULL`, por lo que las ejecuciones activas no deben formar parte de la selección normal.

La purga elimina metadata técnica Spring Batch en orden compatible con las relaciones entre tablas. No elimina `CONTROL_NOMINA`.

### 9.3 Procedimiento seguro

Siempre comenzar con:

```json
{
  "retentionDays": 30,
  "dryRun": true,
  "includeFailed": false
}
```

Revisar los candidatos y las cantidades por tabla antes de evaluar una purga real.

Mientras el Issue #20 permanezca abierto, antes de una purga productiva se debe validar explícitamente que el servicio de purga utiliza el datasource, schema y table prefix efectivos de Spring Batch.

Si existe duda sobre el direccionamiento a Oracle, no ejecutar la purga real.

El procedimiento operativo detallado está en [`runbook.md`](runbook.md).

## 10. Monitoreo de crecimiento y salud técnica

[`sql-queries.md`](sql-queries.md) es el catálogo autoritativo de consultas de soporte. Esta guía no duplica esas consultas.

Revisiones típicas:

| Necesidad | Revisar en `sql-queries.md` |
|---|---|
| últimas ejecuciones | metadata Spring Batch |
| jobs aparentemente activos | estados técnicos activos |
| jobs fallidos | `EXIT_CODE` / `EXIT_MESSAGE` |
| duración de ejecuciones | timestamps Batch |
| metadata antigua | candidatos de antigüedad |
| `PROCESSING` antiguos | `CONTROL_NOMINA` |
| resultado por job | `CONTROL_NOMINA` por `JOB_EXECUTION_ID` |

Indicadores que ameritan investigación:

- crecimiento inesperado de `BATCH_*`;
- gran cantidad de ejecuciones `FAILED` antiguas;
- ejecuciones `STARTED`/`STARTING` durante un tiempo incompatible con la operación normal;
- registros `CONTROL_NOMINA=PROCESSING` antiguos;
- incremento sostenido del tiempo de ejecución;
- errores Oracle repetitivos.

No existe en este documento una periodicidad obligatoria. La frecuencia debe alinearse con la política operativa vigente y el volumen real del servicio.

## 11. Estados anómalos o aparentemente huérfanos

### BATCH_JOB_EXECUTION activo durante demasiado tiempo

No asumir automáticamente que el job sigue procesando.

Antes de intervenir:

1. revisar pod/proceso de aplicación;
2. revisar logs por `jobExecutionId`;
3. revisar timestamps de la ejecución;
4. revisar step executions;
5. determinar si hubo reinicio o caída de infraestructura.

No eliminar la metadata como primer mecanismo de recuperación.

### CONTROL_NOMINA PROCESSING antiguo

No cambiar manualmente el estado a `OK`, `NOK` o `ERROR`.

Reconstruir primero la ejecución mediante:

- `JOB_EXECUTION_ID`;
- logs;
- `BATCH_JOB_EXECUTION`;
- operación Artikos alcanzada;
- llamadas Procurement realizadas;
- estado actual en sistemas externos cuando corresponda.

### FAILED histórico

Un `FAILED` antiguo puede ser evidencia necesaria para soporte y análisis de incidentes.

No eliminarlo solo porque ocupa espacio. Evaluar retención, criticidad y evidencia disponible antes de incluirlo en una purga administrativa.

## 12. Qué revisar antes de modificar Spring Batch

Antes de cambiar la infraestructura o comportamiento del job revisar como mínimo:

- [ ] job afectado;
- [ ] step afectado;
- [ ] unidad de procesamiento;
- [ ] chunk size;
- [ ] `maxNominas` y límites operativos;
- [ ] modelo de concurrencia;
- [ ] transaction manager;
- [ ] datasource;
- [ ] `spring.batch.jdbc.table-prefix`;
- [ ] restartabilidad;
- [ ] interacción con `CONTROL_NOMINA`;
- [ ] estado Artikos y transiciones `NOMFACTERP` / `NOMFACTCONFIR` / `NOMFACTRES`;
- [ ] efectos Procurement/ASI;
- [ ] pruebas unitarias e integración asociadas.

La implementación actual utiliza un `TaskExecutorJobLauncher` asíncrono con un `SimpleAsyncTaskExecutor` y `concurrencyLimit(2)`.

Este valor describe la implementación vigente; no debe considerarse una regla permanente. Cualquier cambio de concurrencia debe revisarse junto con el bloqueo por perfil, los recursos Oracle y los efectos externos.

Para detalles del job consultar [`batch-flow.md`](batch-flow.md) y [`operational-hardening.md`](operational-hardening.md).

## 13. Checklist de mantenimiento técnico

### Revisión rutinaria o bajo demanda

- [ ] revisar crecimiento de `BATCH_*`;
- [ ] revisar ejecuciones `FAILED` relevantes;
- [ ] revisar jobs aparentemente activos;
- [ ] revisar `CONTROL_NOMINA=PROCESSING` antiguos;
- [ ] revisar duración/tendencias de ejecución cuando exista evidencia de degradación;
- [ ] revisar conectividad/permisos Oracle ante errores persistentes;
- [ ] revisar si existen scripts DB pendientes de aplicación por ambiente;
- [ ] registrar cualquier intervención relevante.

### Antes de una purga

- [ ] confirmar que no existen jobs activos involucrados;
- [ ] definir `retentionDays`;
- [ ] mantener `includeFailed=false` salvo decisión explícita;
- [ ] validar datasource/schema/table prefix efectivo;
- [ ] ejecutar primero `dryRun=true`;
- [ ] revisar cantidades y candidatos;
- [ ] obtener autorización correspondiente;
- [ ] ejecutar la purga real solo si la evidencia es consistente;
- [ ] registrar resultado.

### Antes de un cambio DB o Batch

- [ ] Issue asociado;
- [ ] impacto funcional y técnico entendido;
- [ ] script/versionado cuando exista DDL;
- [ ] estrategia de rollback definida cuando corresponda;
- [ ] tests relevantes;
- [ ] Pull Request;
- [ ] plan de validación por ambiente;
- [ ] evidencia post-deploy.

## 14. Brechas técnicas conocidas

### Issue #20 — datasource y table prefix de purga

Existe una brecha abierta para validar que `BatchMetadataPurgeService` utilice inequívocamente el datasource de Spring Batch y respete el schema/table prefix configurado.

Hasta su resolución, una purga productiva requiere validación explícita del direccionamiento antes de ejecutar `dryRun=false`.

### Issue #21 — scripts V001/V002

Existe una brecha abierta para reconciliar la intención histórica de los scripts de `CONTROL_NOMINA` y definir una estrategia inequívoca para baseline e incremental.

Hasta su resolución, no asumir `V001 -> V002` como secuencia de instalación nueva.

Estas brechas deben corregirse mediante Issues técnicos separados; no se resuelven modificando manualmente producción durante tareas de mantenimiento.

## 15. Documentación relacionada

| Necesidad | Documento |
|---|---|
| operar el servicio | [`runbook.md`](runbook.md) |
| diagnosticar incidentes | [`support-guide.md`](support-guide.md) |
| consultar Oracle | [`sql-queries.md`](sql-queries.md) |
| entender el flujo del batch | [`batch-flow.md`](batch-flow.md) |
| revisar controles operativos | [`operational-hardening.md`](operational-hardening.md) |
| revisar ambientes/configuración | [`environments-and-dependencies.md`](environments-and-dependencies.md) |
| release y despliegue | [`release-and-deployment.md`](release-and-deployment.md) |
| onboarding del mantenedor | [`onboarding.md`](onboarding.md) |

## 16. Principio de mantenimiento

El mantenimiento técnico debe preservar tres propiedades:

```text
trazabilidad
+ coherencia entre capas
+ reversibilidad de los cambios controlados
```

Una intervención que elimina evidencia, modifica estados manualmente o cambia estructuras sin un script/versionado asociado dificulta la continuidad operativa y debe evitarse.
