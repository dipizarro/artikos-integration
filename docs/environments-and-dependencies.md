# Ambientes, configuración y dependencias externas

## 1. Objetivo

Este documento describe cómo se relaciona `atk-nomina-batch` con sus ambientes, fuentes de configuración y dependencias externas.

Su propósito es permitir que un mantenedor pueda responder rápidamente:

- en qué ambiente está trabajando;
- qué sistemas externos participan;
- qué configuración necesita la aplicación;
- qué valores son secretos;
- dónde debe administrarse cada configuración;
- qué dependencia revisar ante una falla.

Este documento no contiene credenciales ni reemplaza la documentación técnica de infraestructura.

Para configuración detallada consultar:

[`infra-delivery.md`](infra-delivery.md)

---

## 2. Principio de configuración

La aplicación no debe depender de valores sensibles almacenados dentro del repositorio.

Conceptualmente, la configuración sigue el siguiente modelo:

```text
Repositorio
    |
    | nombres de properties
    | defaults seguros
    |
    v
Configuración del ambiente
    |
    +--> Azure App Configuration
    |       |
    |       +--> parámetros no sensibles
    |
    +--> Azure Key Vault
            |
            +--> tokens
            +--> passwords
            +--> credenciales
```

La aplicación recibe finalmente estos valores mediante la configuración administrada del ambiente.

Nunca se deben versionar:

- tokens Artikos;
- passwords Oracle;
- credenciales Procurement;
- connection strings sensibles;
- archivos locales con credenciales;
- planillas de parámetros que contengan secretos.

---

## 3. Ambientes

Los ambientes relevantes para la continuidad del servicio son:

| Ambiente | Propósito | Integraciones reales |
|---|---|---|
| Local | Desarrollo, pruebas y replay | Configurable |
| QA | Validación integrada | Sí, contra servicios de prueba |
| PRE | Validación previa a producción | Debe confirmarse configuración efectiva |
| PROD | Operación productiva | Sí |

### 3.1 Local

El ambiente local se utiliza para:

- desarrollo;
- pruebas automatizadas;
- replay de XML;
- diagnóstico controlado.

Puede utilizar como fuente:

```properties
artikos.source.mode=local-xml
```

para evitar el consumo de una nómina remota.

También puede ejecutarse contra integraciones remotas cuando exista autorización y configuración apropiada.

Consultar:

- [`local-e2e-testing.md`](local-e2e-testing.md)
- [`artikos-replay-local.md`](artikos-replay-local.md)

### 3.2 QA

QA se utiliza para validaciones integradas antes de promover cambios.

Artikos dispone de servicios específicos para ambiente de pruebas.

La especificación oficial distingue entre:

```text
Obtención de nómina
    -> DocExtractor

Confirmación / resultado
    -> DocConnector
```

Los parámetros funcionales y tokens correspondientes al ambiente deben obtenerse desde los mecanismos corporativos de configuración.

### 3.3 PRE

PRE constituye el ambiente de validación anterior a producción dentro del ciclo de entrega de `artikos-integration`.

La relación exacta entre PRE y los ambientes externos debe confirmarse en la configuración administrada vigente.

En particular debe verificarse:

- endpoint Artikos utilizado;
- endpoint Procurement;
- Oracle;
- gateway;
- secretos asociados;
- observabilidad.

No asumir que PRE utiliza automáticamente las mismas dependencias que QA.

### 3.4 PROD

PROD corresponde al servicio utilizado para procesamiento real de nóminas.

Debe ejecutar las integraciones habilitadas:

```properties
artikos.source.mode=remote
artikos.confirm.enabled=true
artikos.result.enabled=true
procurement.client.enabled=true
procurement.integration.enabled=true
```

Los valores concretos deben provenir de la configuración administrada y no del repositorio.

---

## 4. Perfiles funcionales Artikos

La solución soporta dos perfiles:

```text
VIDA
GENERALES
```

Cada perfil representa una configuración Artikos independiente.

Para cada uno existen parámetros asociados a las tres operaciones:

```text
                    +------------------+
                    |      Perfil      |
                    | VIDA / GENERALES |
                    +--------+---------+
                             |
          +------------------+------------------+
          |                  |                  |
          v                  v                  v
     NOMFACTERP        NOMFACTCONFIR        NOMFACTRES
   Consumo nómina       Confirmación         Resultado
```

Los parámetros de un perfil no deben intercambiarse con los del otro.

---

## 5. Contrato Artikos

La integración utiliza tres acciones principales.

### NOMFACTERP

Solicita una nómina disponible.

Según la especificación Artikos, si existen nóminas pendientes el servicio selecciona la de fecha de procesamiento más antigua.

Si no existen más nóminas, Artikos informa esta situación y el ciclo de consulta debe finalizar.

### NOMFACTCONFIR

Confirma si la nómina obtenida fue correctamente recibida.

Esta operación es relevante para el estado funcional de la cola Artikos.

Si una nómina no queda correctamente confirmada, puede permanecer disponible para una solicitud posterior.

### NOMFACTRES

Informa el resultado del procesamiento de una nómina.

El resultado contiene:

- número de nómina;
- cantidad OK;
- cantidad NOK;
- cantidad informada;
- resultado individual de documentos.

La definición detallada de los mensajes corresponde a la especificación Artikos SAF v1.4.1.

---

## 6. Endpoints Artikos

Artikos separa las responsabilidades en dos Web Services.

### Obtención de nómina

Conceptualmente:

```text
ARTIKOS_NOMINA_URL
        |
        v
AtkWS_DocExtractorB2B
        |
        v
NOMFACTERP
```

### Confirmación y resultado

Conceptualmente:

```text
ARTIKOS_CONNECTOR_URL
        |
        +--> NOMFACTCONFIR
        |
        +--> NOMFACTRES
```

Artikos dispone de endpoints diferentes para pruebas y producción.

Las URLs efectivas utilizadas por la aplicación deben obtenerse desde la configuración vigente del ambiente.

No hardcodear URLs Artikos dentro del código.

---

## 7. Parámetros Artikos

La especificación Artikos define parámetros comunes para la integración.

Entre los más relevantes se encuentran:

| Parámetro | Uso | Secreto |
|---|---|---|
| `Token` | Autenticación/invocación Artikos | **Sí** |
| `MsgCode` | Acción solicitada | No |
| `MsgFromAddress` | Identificación del origen | No |
| `MsgCodFromAddress` | Identificación de empresa | No |
| `MsgToAddress` | Destino del mensaje | No |
| `MsgCodSis` | Sistema de origen | No |

El contrato utiliza:

```text
MsgToAddress = ARTIKOS
MsgCodSis    = SAF
```

para este modelo de integración, sujeto siempre a la configuración entregada oficialmente para la empresa/perfil.

Los tokens proporcionados por Artikos son secretos y deben administrarse fuera de Git.

---

## 8. Variables principales de la aplicación

### Oracle — datasource funcional/JPA

```text
APP_DATASOURCE_URL
APP_DATASOURCE_USERNAME
APP_DATASOURCE_PASSWORD
APP_DATASOURCE_DRIVER_CLASS_NAME
APP_DB_SCHEMA
```

Este datasource se utiliza para la persistencia funcional de aplicación y lookups ASI, incluyendo `CONTROL_NOMINA`, `GRL_MAE_ITEM` y `GRL_MAE_ITEM_DET`.

### Oracle — datasource Spring Batch

```text
BATCH_DATASOURCE_URL
BATCH_DATASOURCE_USERNAME
BATCH_DATASOURCE_PASSWORD
BATCH_DATASOURCE_DRIVER_CLASS_NAME
SPRING_BATCH_JDBC_TABLE_PREFIX
```

Este datasource se utiliza para el `JobRepository` y metadata técnica `BATCH_*`.

La configuración base usa `spring.batch.jdbc.initialize-schema=never`, por lo que la aplicación no crea automáticamente las tablas Batch al arrancar.

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

### Tokens Artikos — GENERALES

```text
ARTIKOS_GENERALES_CONSUMO_TOKEN
ARTIKOS_GENERALES_RESPUESTA_TOKEN
ARTIKOS_GENERALES_RESULTADO_TOKEN
```

### Tokens Artikos — VIDA

```text
ARTIKOS_VIDA_CONSUMO_TOKEN
ARTIKOS_VIDA_RESPUESTA_TOKEN
ARTIKOS_VIDA_RESULTADO_TOKEN
```

### Parámetros funcionales Artikos

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

Para descripción técnica y permisos consultar:

[`infra-delivery.md`](infra-delivery.md)

---

## 9. Clasificación de configuración

### Secretos

Deben almacenarse mediante un mecanismo seguro como Azure Key Vault.

| Configuración | Clasificación |
|---|---|
| Tokens Artikos | Secreto |
| Password Oracle | Secreto |
| Credenciales Procurement | Secreto |
| Credenciales técnicas externas | Secreto |

### Configuración no sensible

Puede mantenerse mediante Azure App Configuration u otro mecanismo corporativo aprobado.

| Configuración | Clasificación |
|---|---|
| URL Artikos | Configuración |
| URL Procurement | Configuración |
| `MsgCode` | Configuración |
| `MsgCodFromAddress` | Configuración |
| `MsgCodExterno` | Configuración |
| límites batch | Configuración |
| timeouts | Configuración |
| flags de integración | Configuración |

La clasificación anterior no reemplaza las políticas corporativas de seguridad.

---

## 10. Oracle

El adapter utiliza Oracle directamente para tres responsabilidades diferentes.

### Control funcional

```text
CONTROL_NOMINA
```

Permisos esperados:

```text
SELECT
INSERT
UPDATE
```

### Metadata técnica Spring Batch

```text
BATCH_*
```

Spring Batch requiere acceso de lectura/escritura sobre su metadata.

Los permisos de eliminación dependen de la política de purga.

### Lookup ASI

```text
GRL_MAE_ITEM
GRL_MAE_ITEM_DET
```

El adapter utiliza estos objetos únicamente para consulta.

La inserción del documento contable final en ASI es responsabilidad de Procurement.

---

## 11. Procurement

Procurement es una dependencia REST de `atk-nomina-batch`.

Flujo:

```text
Documento Artikos
      |
      v
Mapping
      |
      v
Contrato Procurement
      |
      v
POST /document
      |
      v
Procurement
      |
      v
ASI
```

La URL efectiva se resuelve mediante:

```text
PROCUREMENT_BASE_URL
```

Cuando:

```text
PROCUREMENT_INTEGRATION_ENABLED=false
```

la aplicación puede iniciar, pero el envío real de documentos queda deshabilitado.

Esta propiedad debe verificarse especialmente ante una situación donde el batch procesa nóminas pero no se observan documentos enviados.

---

## 12. Dependencias externas

| Dependencia | Interfaz | Responsabilidad | Fallas habituales |
|---|---|---|---|
| Artikos | SOAP | Entrega, confirmación y resultado de nóminas | red, timeout, estado funcional |
| Procurement | REST | Recepción del documento contable | HTTP, contrato, rechazo funcional |
| Oracle / ASI | JDBC | Control, Batch y lookup | conectividad, permisos, datos |
| Azure App Configuration | Configuración | Parámetros por ambiente | ausencia o valor incorrecto |
| Azure Key Vault | Secretos | Tokens y credenciales | permisos, referencia o secreto |
| Gateway / CONC-Kong | HTTP | Exposición de la API | routing, autorización |
| Kubernetes | Runtime | Ejecución del servicio | pod, recursos, despliegue |
| Observabilidad | Logs/APM | Diagnóstico | pérdida o falta de correlación |

---

## 13. Ownership operativo

La primera clasificación de una falla debe identificar qué sistema posee la responsabilidad.

```text
¿Dónde está la falla?
       |
       +--> atk-nomina-batch
       |       -> equipo mantenedor
       |
       +--> Artikos
       |       -> proveedor / responsable Artikos
       |
       +--> Procurement
       |       -> equipo Procurement
       |
       +--> Oracle / ASI
       |       -> ASI / DBA
       |
       +--> Kubernetes / red
       |       -> Infraestructura
       |
       +--> Gateway
       |       -> responsable API / Gateway
       |
       +--> Azure configuration
               -> plataforma / Infra
```

Evitar versionar nombres personales como ownership principal.

La documentación debe mantenerse válida aunque cambien integrantes de los equipos.

---

## 14. Checklist de validación de ambiente

Antes de realizar una prueba integrada o habilitar un ambiente verificar:

### Aplicación

- [ ] perfil Spring correcto;
- [ ] aplicación inicia correctamente;
- [ ] `/actuator/health` disponible.

### Artikos

- [ ] `ARTIKOS_NOMINA_URL`;
- [ ] `ARTIKOS_CONNECTOR_URL`;
- [ ] configuración VIDA;
- [ ] configuración GENERALES;
- [ ] tokens disponibles mediante mecanismo seguro;
- [ ] confirmación habilitada;
- [ ] resultado habilitado.

### Procurement

- [ ] `PROCUREMENT_BASE_URL`;
- [ ] conectividad;
- [ ] integración habilitada.

### Oracle

- [ ] `APP_DATASOURCE_*` configurado y accesible;
- [ ] `BATCH_DATASOURCE_*` configurado y accesible;
- [ ] `APP_DB_SCHEMA` correcto;
- [ ] `SPRING_BATCH_JDBC_TABLE_PREFIX` correcto;
- [ ] `CONTROL_NOMINA`;
- [ ] `BATCH_*`;
- [ ] `GRL_MAE_ITEM`;
- [ ] `GRL_MAE_ITEM_DET`;
- [ ] permisos de los usuarios de servicio.

### Plataforma

- [ ] configuración del ambiente;
- [ ] secretos;
- [ ] gateway;
- [ ] Kubernetes;
- [ ] logs / observabilidad.

---

## 15. Diagnóstico por dependencia

### El batch no logra obtener nómina

Revisar:

```text
ARTIKOS_NOMINA_URL
token del perfil
MsgCode NOMFACTERP
conectividad
timeout
respuesta Artikos
```

### La nómina se obtiene pero falla la confirmación

Revisar:

```text
ARTIKOS_CONNECTOR_URL
token de confirmación
NOMFACTCONFIR
estado de la nómina en Artikos
```

### Los documentos no llegan a ASI

Separar el análisis:

```text
¿atk-nomina-batch llamó Procurement?
              |
         +----+----+
         |         |
        no        sí
         |         |
 configuración   revisar
 / mapping       respuesta
 / aplicación    Procurement
```

No asumir inicialmente que una ausencia en ASI implica una falla del adapter.

### Falla acceso Oracle

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

Una conexión correcta del datasource funcional no demuestra por sí sola que Spring Batch pueda acceder a su metadata, ni viceversa.

---

## 16. Documentación externa Artikos

La implementación fue construida considerando documentación entregada por Artikos.

Referencias conocidas:

### Especificación funcional/técnica

```text
Especificación de Modelo de Integración con ERP
desde Sistema de Administración de Facturas (SAF)

Versión 1.4.1
```

Contiene:

- modelo funcional;
- `NOMFACTERP`;
- `NOMFACTCONFIR`;
- `NOMFACTRES`;
- contratos XML;
- estructura de nómina;
- respuesta genérica de Web Services.

### Parámetros QA

Existe documentación proporcionada por Artikos con parámetros específicos del ambiente QA para los perfiles VIDA y GENERALES.

Este recurso contiene secretos.

**No versionar en Git.**

### Parámetros PRD

Existe documentación proporcionada por Artikos con parámetros específicos del ambiente productivo para los perfiles VIDA y GENERALES.

Este recurso contiene secretos.

**No versionar en Git.**

Ante una diferencia entre parámetros históricos y configuración actualmente desplegada, la configuración administrada vigente debe validarse antes de realizar cambios.

---

## 17. Fuentes de verdad

Para evitar inconsistencias, utilizar la siguiente prioridad:

```text
Comportamiento de aplicación
        -> código + arquitectura

Contrato Artikos
        -> especificación oficial Artikos

Valores por ambiente
        -> configuración administrada vigente

Secretos
        -> Key Vault / mecanismo corporativo

Infraestructura
        -> infra-delivery + repositorio IaC

Operación
        -> runbook / support-guide
```

Nunca utilizar una copia local antigua de parámetros como única fuente para modificar producción.

---

## 18. Documentación relacionada

| Necesidad | Documento |
|---|---|
| Arquitectura | [`architecture.md`](architecture.md) |
| Onboarding | [`onboarding.md`](onboarding.md) |
| Infraestructura | [`infra-delivery.md`](infra-delivery.md) |
| Mantenimiento técnico | [`technical-maintenance.md`](technical-maintenance.md) |
| Release y despliegue | [`release-and-deployment.md`](release-and-deployment.md) |
| Handover | [`handover-checklist.md`](handover-checklist.md) |
| Gateway | [`gateway-endpoints.md`](gateway-endpoints.md) |
| Operación | [`runbook.md`](runbook.md) |
| Troubleshooting | [`support-guide.md`](support-guide.md) |
| SQL soporte | [`sql-queries.md`](sql-queries.md) |
| Pruebas remotas Artikos | [`artikos-remote-e2e.md`](artikos-remote-e2e.md) |

---

## 19. Información pendiente de validación

La siguiente información debe confirmarse contra la plataforma actualmente desplegada:

- relación exacta de PRE con los ambientes externos Artikos y Procurement;
- ubicación/nombre definitivo de las configuraciones administradas;
- ownership corporativo vigente de cada dependencia;
- cualquier diferencia entre los parámetros Artikos entregados originalmente y la configuración actualmente desplegada;
- baseline productivo para inicializar las ramas corporativas `preproduccion` y `produccion`.

Estas validaciones no deben resolverse mediante suposiciones ni copiando directamente parámetros históricos.