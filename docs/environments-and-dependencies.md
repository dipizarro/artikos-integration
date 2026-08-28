# Ambientes, configuración y dependencias externas

## 1. Objetivo

Este documento describe cómo se relaciona `atk-nomina-batch` con sus ambientes, fuentes de configuración y dependencias externas.

Debe permitir a un mantenedor identificar:

- en qué ambiente está trabajando;
- qué sistemas externos participan;
- qué configuración necesita la aplicación;
- qué valores son secretos;
- dónde se administra cada configuración;
- qué dependencia revisar ante una falla.

Para configuración de infraestructura consultar [`infra-delivery.md`](infra-delivery.md).

## 2. Principio de configuración

La aplicación no debe depender de secretos almacenados en el repositorio.

```text
Repositorio
    |
    | properties + defaults seguros
    v
Configuración del ambiente
    |
    +--> Azure App Configuration
    |       -> parámetros no sensibles
    |
    +--> Azure Key Vault
            -> tokens
            -> passwords
            -> credenciales
```

Nunca versionar:

- tokens Artikos;
- passwords Oracle;
- credenciales Procurement;
- connection strings sensibles;
- archivos locales con credenciales;
- planillas de parámetros que contengan secretos.

## 3. Ambientes

| Ambiente | Propósito | Integraciones reales |
|---|---|---|
| Local | Desarrollo, tests y replay | Configurable |
| QA | Validación integrada | Servicios de prueba |
| PRE | Validación previa a producción | Confirmar configuración efectiva |
| PROD | Operación productiva | Sí |

### Local

Puede utilizar:

```properties
artikos.source.mode=local-xml
```

para evitar consumo remoto de nóminas.

Consultar [`local-e2e-testing.md`](local-e2e-testing.md) y [`artikos-replay-local.md`](artikos-replay-local.md).

### QA

Se utiliza para validaciones integradas. Los parámetros y tokens deben provenir de los mecanismos corporativos de configuración.

### PRE

No asumir que PRE utiliza automáticamente las mismas dependencias que QA. Confirmar:

- endpoint Artikos;
- endpoint Procurement;
- Oracle;
- gateway;
- secretos;
- observabilidad.

### PROD

Para procesamiento real deben estar alineadas, entre otras, estas propiedades:

```properties
artikos.source.mode=remote
artikos.confirm.enabled=true
artikos.result.enabled=true
procurement.client.enabled=true
procurement.integration.enabled=true
```

Los valores concretos deben provenir de configuración administrada.

## 4. Perfiles Artikos

La solución soporta:

```text
VIDA
GENERALES
```

Cada perfil posee configuración independiente para:

```text
NOMFACTERP
NOMFACTCONFIR
NOMFACTRES
```

Los parámetros de un perfil no deben intercambiarse con los del otro.

## 5. Contrato Artikos

### NOMFACTERP

Solicita una nómina disponible. Cuando no existen más nóminas, Artikos informa esa situación y el ciclo debe finalizar.

### NOMFACTCONFIR

Confirma la correcta recepción de la nómina y afecta su estado funcional en Artikos.

### NOMFACTRES

Informa el resultado consolidado del procesamiento.

La definición contractual detallada corresponde a la especificación Artikos SAF v1.4.1.

## 6. Endpoints Artikos

```text
ARTIKOS_NOMINA_URL
        |
        v
DocExtractorB2B
        |
        v
NOMFACTERP
```

```text
ARTIKOS_CONNECTOR_URL
        |
        +--> NOMFACTCONFIR
        +--> NOMFACTRES
```

Las URLs efectivas deben obtenerse desde la configuración vigente del ambiente. No hardcodear endpoints productivos en código o documentación operativa.

## 7. Parámetros Artikos

Entre los parámetros principales se encuentran:

| Parámetro | Uso | Secreto |
|---|---|---|
| `Token` | Autenticación/invocación | Sí |
| `MsgCode` | Acción solicitada | No |
| `MsgFromAddress` | Origen | No |
| `MsgCodFromAddress` | Identificación empresa | No |
| `MsgToAddress` | Destino | No |
| `MsgCodSis` | Sistema origen | No |

Los tokens deben administrarse fuera de Git.

## 8. Variables principales de la aplicación

### Oracle — datasource funcional

```text
APP_DATASOURCE_URL
APP_DATASOURCE_USERNAME
APP_DATASOURCE_PASSWORD
APP_DATASOURCE_DRIVER_CLASS_NAME
APP_DB_SCHEMA
```

Responsabilidades:

- persistencia JPA de aplicación;
- `CONTROL_NOMINA`;
- `GRL_MAE_ITEM`;
- `GRL_MAE_ITEM_DET`.

### Oracle — datasource Spring Batch

```text
BATCH_DATASOURCE_URL
BATCH_DATASOURCE_USERNAME
BATCH_DATASOURCE_PASSWORD
BATCH_DATASOURCE_DRIVER_CLASS_NAME
SPRING_BATCH_JDBC_TABLE_PREFIX
```

Responsabilidades:

- `JobRepository` Spring Batch;
- metadata `BATCH_*`.

La aplicación configura por defecto:

```properties
spring.batch.jdbc.initialize-schema=never
spring.batch.jdbc.table-prefix=BACHPROCESS.BATCH_
```

Los valores efectivos pueden variar por ambiente y deben validarse contra la configuración administrada.

### Procurement

```text
PROCUREMENT_BASE_URL
PROCUREMENT_INTEGRATION_ENABLED
```

### Artikos

```text
ARTIKOS_NOMINA_URL
ARTIKOS_CONNECTOR_URL
```

### Tokens Artikos GENERALES

```text
ARTIKOS_GENERALES_CONSUMO_TOKEN
ARTIKOS_GENERALES_RESPUESTA_TOKEN
ARTIKOS_GENERALES_RESULTADO_TOKEN
```

### Tokens Artikos VIDA

```text
ARTIKOS_VIDA_CONSUMO_TOKEN
ARTIKOS_VIDA_RESPUESTA_TOKEN
ARTIKOS_VIDA_RESULTADO_TOKEN
```

### Parámetros funcionales

```text
ARTIKOS_GENERALES_MSG_COD_FROM_ADDRESS
ARTIKOS_GENERALES_MSG_COD_EXTERNO
ARTIKOS_VIDA_MSG_COD_FROM_ADDRESS
ARTIKOS_VIDA_MSG_COD_EXTERNO
```

### Límites y conectividad

```text
ATK_BATCH_DEFAULT_MAX_NOMINAS
ATK_BATCH_MAX_NOMINAS_PER_RUN
ARTIKOS_HTTP_CONNECT_TIMEOUT_MS
ARTIKOS_HTTP_READ_TIMEOUT_MS
```

## 9. Clasificación de configuración

### Secretos

- passwords y usuarios sensibles Oracle;
- tokens Artikos;
- credenciales Procurement;
- credenciales técnicas externas;
- connection strings cuando la política corporativa las clasifique como sensibles.

### Configuración no sensible

- URLs de servicios;
- `MsgCode` y códigos funcionales;
- límites batch;
- timeouts;
- flags de integración;
- schema/prefix cuando no revelen información sensible según política corporativa.

## 10. Oracle

El adapter utiliza Oracle directamente para tres responsabilidades.

### Control funcional

```text
CONTROL_NOMINA
```

Permisos esperados: `SELECT`, `INSERT`, `UPDATE`.

### Metadata Spring Batch

```text
BATCH_*
```

Spring Batch requiere lectura/escritura sobre su metadata. Los permisos `DELETE` dependen de la política de purga.

### Lookup ASI

```text
GRL_MAE_ITEM
GRL_MAE_ITEM_DET
```

El adapter los utiliza para consulta. La inserción final del documento contable en ASI es responsabilidad de Procurement.

Para mantenimiento consultar [`technical-maintenance.md`](technical-maintenance.md).

## 11. Procurement

```text
Documento Artikos
      |
      v
Mapping
      |
      v
POST /api/v1/document
      |
      v
Procurement
      |
      v
ASI
```

Si `PROCUREMENT_INTEGRATION_ENABLED=false`, la aplicación puede iniciar pero no realizará envío real de documentos.

## 12. Dependencias externas

| Dependencia | Interfaz | Responsabilidad | Fallas habituales |
|---|---|---|---|
| Artikos | SOAP | Entrega, confirmación y resultado | red, timeout, estado funcional |
| Procurement | REST | Recepción documento contable | HTTP, contrato, rechazo funcional |
| Oracle / ASI | JDBC | Control, Batch y lookup | conectividad, permisos, datos |
| Azure App Configuration | Config | Parámetros ambiente | ausencia/valor incorrecto |
| Azure Key Vault | Secretos | Tokens y credenciales | permisos/referencia/secreto |
| CONC/Kong | HTTP | Exposición API | routing/autorización |
| Kubernetes / Flux | Runtime/deploy | Ejecución servicio | pod/deploy/recursos |
| Observabilidad | Logs/APM | Diagnóstico | falta de correlación/evidencia |

## 13. Ownership operativo

```text
atk-nomina-batch   -> equipo mantenedor
Artikos            -> proveedor/responsable Artikos
Procurement        -> equipo Procurement
Oracle / ASI       -> ASI / DBA
Kubernetes / red   -> Infraestructura
Gateway            -> responsable API/Gateway
Azure config       -> plataforma / Infra
```

Evitar usar nombres personales como ownership principal.

## 14. Checklist de validación de ambiente

### Aplicación

- [ ] perfil Spring correcto;
- [ ] aplicación inicia;
- [ ] `/actuator/health` disponible.

### Artikos

- [ ] endpoints correctos;
- [ ] configuración VIDA;
- [ ] configuración GENERALES;
- [ ] tokens disponibles por mecanismo seguro;
- [ ] confirmación y resultado habilitados cuando corresponda.

### Procurement

- [ ] base URL correcta;
- [ ] conectividad;
- [ ] integración habilitada según ambiente.

### Oracle

- [ ] `APP_DATASOURCE_*` configurado;
- [ ] `BATCH_DATASOURCE_*` configurado;
- [ ] `APP_DB_SCHEMA` correcto;
- [ ] `SPRING_BATCH_JDBC_TABLE_PREFIX` correcto;
- [ ] acceso a `CONTROL_NOMINA`;
- [ ] acceso a `BATCH_*`;
- [ ] acceso a lookups ASI;
- [ ] permisos adecuados.

### Plataforma

- [ ] App Configuration;
- [ ] Key Vault;
- [ ] gateway;
- [ ] Kubernetes/Flux;
- [ ] logs/observabilidad.

## 15. Diagnóstico por dependencia

### No obtiene nómina

Revisar `ARTIKOS_NOMINA_URL`, token/perfil, `NOMFACTERP`, red, timeout y respuesta Artikos.

### Obtiene nómina pero falla confirmación

Revisar `ARTIKOS_CONNECTOR_URL`, token de confirmación, `NOMFACTCONFIR` y estado de la nómina.

### Documentos no llegan a ASI

Primero determinar si `atk-nomina-batch` llamó Procurement. Si hubo respuesta satisfactoria del adapter hacia Procurement, continuar el análisis en Procurement/ASI según ownership.

### Falla Oracle

Revisar separadamente:

```text
APP_DATASOURCE_*
BATCH_DATASOURCE_*
APP_DB_SCHEMA
SPRING_BATCH_JDBC_TABLE_PREFIX
secreto/password
red
permisos
```

No asumir que una conexión correcta del datasource funcional demuestra que Spring Batch puede acceder a su metadata, ni viceversa.

## 16. Documentación externa Artikos

Referencias conocidas:

```text
Especificación de Modelo de Integración con ERP
desde Sistema de Administración de Facturas (SAF)
Versión 1.4.1
```

Las planillas de parámetros QA/PRD proporcionadas por Artikos contienen secretos y no deben versionarse en Git.

Ante diferencias entre parámetros históricos y configuración desplegada, validar la configuración administrada vigente.

## 17. Fuentes de verdad

```text
Comportamiento app   -> código + arquitectura
Contrato Artikos     -> especificación oficial
Valores por ambiente -> configuración administrada vigente
Secretos             -> Key Vault / mecanismo corporativo
Infraestructura      -> infra-delivery + IaC
Operación            -> runbook / support-guide
Mantenimiento        -> technical-maintenance
Release              -> release-and-deployment
```

## 18. Documentación relacionada

| Necesidad | Documento |
|---|---|
| Arquitectura | [`architecture.md`](architecture.md) |
| Onboarding | [`onboarding.md`](onboarding.md) |
| Infraestructura | [`infra-delivery.md`](infra-delivery.md) |
| Mantenimiento | [`technical-maintenance.md`](technical-maintenance.md) |
| Release | [`release-and-deployment.md`](release-and-deployment.md) |
| Operación | [`runbook.md`](runbook.md) |
| Troubleshooting | [`support-guide.md`](support-guide.md) |
| SQL soporte | [`sql-queries.md`](sql-queries.md) |
| Handover | [`handover-checklist.md`](handover-checklist.md) |

## 19. Información pendiente de validación externa

Debe confirmarse contra la plataforma desplegada:

- relación exacta de PRE con ambientes externos;
- nombres/ubicaciones definitivas de configuración administrada;
- ownership corporativo vigente;
- diferencias entre parámetros históricos Artikos y configuración actual;
- baseline productivo requerido para inicializar ramas corporativas `preproduccion`/`produccion`.

Estas validaciones no deben resolverse mediante suposiciones.