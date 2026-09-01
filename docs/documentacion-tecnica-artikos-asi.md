# Documentación técnica — Integración Artikos / Procurement / ASI

## 1. Introducción

### 1.1 Propósito

Este documento describe la solución implementada en el proyecto `atk-nomina-batch` del repositorio `artikos-integration`. La aplicación es un adapter batch construido con Java 17, Spring Boot 3.3.5, Spring Batch, Spring Data JPA y Oracle. Su responsabilidad es obtener nóminas de documentos contables desde Artikos, confirmar su recepción, transformar y enviar cada documento a Procurement, y comunicar a Artikos el resultado consolidado.

El contenido se basa exclusivamente en código, propiedades, migraciones, pruebas, muestras, manifiestos y documentación versionada en el repositorio. Los ejemplos están sanitizados. No se incluyen tokens, contraseñas, usuarios, cadenas JDBC ni credenciales reales.

### 1.2 Alcance

El documento cubre la arquitectura, componentes, flujo funcional, contratos, persistencia, configuración, manejo de errores, idempotencia, despliegue, validación, consideraciones productivas y diagnóstico técnico de la solución.

Una precisión de alcance: el código del adapter no inserta directamente documentos en tablas Procurement/ASI. Envía un contrato JSON CMP al endpoint de Procurement; la persistencia documental en ASI es responsabilidad del servicio Procurement. El adapter sí accede directamente a `CONTROL_NOMINA`, `GRL_MAE_ITEM`, `GRL_MAE_ITEM_DET` y a la metadata `BATCH_*`.

Los procedimientos operativos detallados, mantenimiento, troubleshooting extendido, ambientes y ciclo de release/despliegue se mantienen en documentos especializados del repositorio y se referencian al final para evitar duplicación.

### 1.3 Audiencia

Este documento está dirigido principalmente a desarrolladores, mantenedores, personal de soporte técnico, arquitectura, DevOps y equipos responsables de la continuidad de las integraciones Artikos, Procurement y ASI.

### 1.4 Estado y baseline documental

La presente documentación representa el estado **as-built** de la solución al cierre del proyecto y debe utilizarse como referencia técnica del sistema productivo.

- Repositorio de referencia de entrega: `artikos-integration` (GitLab cliente).
- Fuente maestra editable: `docs/documentacion-tecnica-artikos-asi.md`.
- Branch de referencia: `main`.
- Tag productivo de referencia: `v1.0.0`.
- Estado de la solución: productiva / cierre de proyecto.

El archivo Word de entrega generado a partir de esta documentación constituye un artefacto formal de publicación. Las modificaciones futuras deben realizarse primero sobre esta fuente versionada y luego regenerar una nueva versión del entregable.

## 2. Arquitectura de la solución

### 2.1 Vista general

```text
Usuario/sistema
    |
    | POST /api/v1/nominas/batch/start
    v
NominaBatchStartController
    |
    v
BatchLauncherService -> JobLauncher asíncrono
    |
    v
nominaDocumentosContablesJob / processNominaDocumentosStep
    |
    +-- Reader -> Artikos NOMFACTERP (SOAP)
    +-- Processor
    |     +-- CONTROL_NOMINA = PROCESSING (APP datasource)
    |     +-- Artikos NOMFACTCONFIR (SOAP)
    |     +-- parsing, homologación ASI y mapping
    |     +-- Procurement POST document (REST, uno por documento)
    |     +-- generación NOMFACTRES
    +-- Writer
          +-- Artikos NOMFACTRES (SOAP)
          +-- CONTROL_NOMINA = OK/NOK/ERROR

Spring Batch -> metadata BATCH_* (BATCH datasource)
Procurement -> inserción documental ASI (fuera del código de este adapter)
```

La versión Word incorpora adicionalmente la figura formal de arquitectura almacenada en `docs/diagramas/arquitectura-artikos-procurement-asi.svg`.

### 2.2 Límites y responsabilidades

| Componente | Responsabilidad principal |
| --- | --- |
| Artikos | Publicar nóminas y recibir confirmación y resultado consolidado |
| `atk-nomina-batch` / `artikos-integration` | Orquestar el proceso, transformar datos, aplicar mapping/homologación, controlar estado e integrar sistemas |
| Procurement | Recibir el contrato CMP y gestionar la persistencia documental hacia ASI |
| ASI / Oracle | Proveer homologaciones y persistir/controlar información funcional y técnica según la responsabilidad de cada componente |

El límite más importante es que `artikos-integration` no ejecuta la inserción documental final en ASI. Su responsabilidad termina en la construcción y envío del contrato esperado por Procurement, además de sus propios controles y lookups Oracle.

### 2.3 Modelo de ejecución

La unidad del step es una nómina completa (`ArtikosFetchedNomina`). El step es chunk-oriented y usa el transaction manager del datasource APP. El tamaño se configura con `atk.batch.real.chunk-size`; el valor versionado es 1, coherente con una confirmación y un resultado remoto por nómina.

El lanzamiento es asíncrono mediante `TaskExecutorJobLauncher` y `SimpleAsyncTaskExecutor`, con límite de concurrencia 2. Antes de iniciar, `BatchConcurrencyService` impide dos ejecuciones activas del mismo job para el mismo perfil (`STARTING`, `STARTED` o `STOPPING`). VIDA y GENERALES pueden, por diseño, ocupar los dos cupos.

## 3. Componentes principales

### 3.1 Controllers

| Componente | Responsabilidad | Condición |
| --- | --- | --- |
| `NominaBatchStartController` | Contrato productivo para iniciar el batch | Siempre cargado |
| `NominaBatchOperationsController` | Estado, resumen y resultado por nómina | `app.endpoints.operations.enabled=true` |
| `ControlNominaController` | Consultas operativas de `CONTROL_NOMINA` | misma condición |
| `HealthController` | Health propio con hora Oracle | misma condición |
| `BatchMetadataAdminController` | Purga controlada de metadata batch | `app.admin.enabled=true` |
| `ArtikosDiagnosticController` | Pruebas directas de las tres operaciones SOAP | `app.diagnostics.enabled=true` |
| `ArtikosConfigController` | Configuración Artikos enmascarada | diagnóstico habilitado |
| `ProcurementDiagnosticController` | Mapping y POST de un documento | diagnóstico habilitado |
| `ControlNominaDiagnosticController` | Prueba de persistencia Oracle | diagnóstico habilitado |

Los endpoints de diagnóstico y administración están deshabilitados por defecto y no deben exponerse en producción.

### 3.2 Services

- `BatchLauncherService`: normaliza parámetros, valida perfil y límite, verifica concurrencia y lanza el job.
- `NominaProcessingService`: procesa los documentos de una nómina, calcula totales y genera `NOMFACTRES`.
- `DocumentProcessingService`: interfaz con implementaciones real Procurement y simulada.
- `ProcurementDocumentProcessingService`: mapea, publica y convierte la respuesta Procurement a resultado Artikos.
- `ProcurementIntegrationService`: orquesta el POST y clasifica errores de cliente/mapping como errores de integración.
- `ControlNominaService`: persistencia transaccional `REQUIRES_NEW` de estados y totales.
- `NominaReprocessingPolicyService`: decide si omitir el reenvío a Procurement cuando la última ejecución de la nómina quedó `OK`.
- `NominaErrorPolicyService`: decide qué errores marcan control y fallan el job; compacta mensajes a 500 caracteres.
- `NominaXmlParserService` y `NominaResultXmlService`: parsing del XML recibido y construcción del resultado.
- `BatchStatusService`, `BatchSummaryService` y `BatchResultStore`: consulta de metadata y resultados. El store de resultados es en memoria y no es durable entre reinicios.

### 3.3 Job, step, readers, processors y writers

- Job: `nominaDocumentosContablesJob`.
- Step activo: `processNominaDocumentosStep`.
- Reader: `ArtikosNominaItemReader`. Invoca la fuente hasta que no existan nóminas o se alcance `maxNominas`.
- Fuentes: `RemoteArtikosNominaSource` para SOAP y `LocalXmlArtikosNominaSource` para replay local.
- Processor: `ArtikosNominaItemProcessor`. Aplica idempotencia, registra `PROCESSING`, confirma, procesa documentos y crea el resultado.
- Writer: `ArtikosNominaResultItemWriter`. Envía `NOMFACTRES`, persiste el estado final y guarda el resultado en memoria.

Existen `NominaItemReader`, `NominaDocumentoItemReader`, `NominaItemProcessor`, `NominaDocumentoItemProcessor` y `NominaResultItemWriter` para simulación/pruebas; no forman el step real configurado.

### 3.4 Clientes externos

`ArtikosSoapClient` usa HTTP SOAP 1.1, builders independientes para `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`, parsers de respuesta y política configurable de reintentos. Las operaciones de consulta usan el endpoint extractor; confirmación y resultado usan el connector.

`ProcurementClient` serializa `ProcurementDocumentRequest`, construye `base-url + document-path`, aplica timeouts y parsea `ProcurementApiResponse`. Un HTTP 2xx sólo es éxito funcional si `statusCode=0`; la capa posterior normaliza duplicados.

### 3.5 Repositories JPA

- `ControlNominaJpaRepository`: CRUD y consultas por ejecución/nómina; obtiene la ejecución más reciente por número de nómina.
- `GrlMaeItemDetRepository`: busca homologaciones por empresa, cuenta, sistema e impuesto.
- `GrlMaeItemRepository`: valida que el maestro del ítem/período esté vigente.

## 4. Flujo funcional detallado

1. Un usuario o sistema invoca `POST /api/v1/nominas/batch/start`.
2. El adapter responde HTTP 202 con el identificador de ejecución y continúa en segundo plano.
3. El reader consulta Artikos con `NOMFACTERP` para VIDA o GENERALES.
4. Si Artikos informa que no existen nóminas, el reader termina y el job queda `COMPLETED`. `maxNominas` es sólo un límite de seguridad.
5. Para una nómina nueva, el processor registra `CONTROL_NOMINA=PROCESSING` con `JOB_EXECUTION_ID`, `NUMERO_NOMINA` y empresa.
6. Si no es `dryRun`, la fuente es remota y la confirmación está habilitada, envía `NOMFACTCONFIR` con `EstadoRespuesta=0`.
7. Se recorren los documentos. El mapper deriva campos desde el XML y consulta homologaciones en `GRL_MAE_ITEM_DET`, validando el maestro vigente en `GRL_MAE_ITEM`.
8. Con integración habilitada se efectúa un POST por documento a Procurement. Procurement es el componente que inserta en ASI; el adapter no contiene esa sentencia SQL.
9. `statusCode=0` es OK; `-20` o un duplicado reconocido es OK idempotente; otro rechazo funcional es NOK.
10. Se consolida `CantidadInformados`, `CantidadOK` y `CantidadNOK`, y se construye el XML `NOMFACTRES`.
11. El writer envía `NOMFACTRES` a Artikos, salvo en dry-run, modo XML local o si el envío está deshabilitado.
12. Si Artikos acepta el resultado, `CONTROL_NOMINA` termina `OK` cuando no hay documentos NOK o `NOK` cuando existe al menos uno.
13. Un rechazo funcional de Procurement, un error técnico Procurement o un error de mapping ocurrido dentro del procesamiento individual se convierte en un resultado documental `NOK`; el loop continúa con los documentos siguientes y el resumen se informa mediante `NOMFACTRES`. En cambio, un error que impida construir un resultado confiable de la nómina —por ejemplo, una falla de confirmación, de procesamiento fuera de la frontera documental, de persistencia de control o de envío del resultado— puede marcar `ERROR`, fallar el job y evitar el envío de `NOMFACTRES`.

Artikos presenta estados previos relevantes observados en la documentación y pruebas del repo: `NOMFACTCONFIR` requiere la nómina en “En Integración” y `NOMFACTRES` en “Recibida”. Un rechazo funcional se expresa con `MsgStatus=1`.

## 5. Endpoint REST principal

### `POST /api/v1/nominas/batch/start`

Content-Type: `application/json`. El body puede omitirse; en tal caso el código usa `GENERALES`, el máximo por defecto y `dryRun=true`. Si se entrega body, `profile` es obligatorio y admite `VIDA` o `GENERALES`; `maxNominas` debe ser al menos 1 y no superar el máximo configurado; `dryRun` nulo equivale a `false`.

> **IMPORTANTE:** la omisión completa del body utiliza `dryRun=true`. En cambio, cuando se envía un body y se omite `dryRun`, el valor efectivo es `false`. Por seguridad operacional, se recomienda informar `dryRun` explícitamente en toda invocación con body.

```json
{
  "profile": "VIDA",
  "maxNominas": 10,
  "dryRun": false
}
```

Respuesta HTTP 202:

```json
{
  "jobExecutionId": 123,
  "jobName": "nominaDocumentosContablesJob",
  "status": "STARTING",
  "message": "Batch iniciado correctamente",
  "profile": "VIDA",
  "maxNominas": 10,
  "dryRun": false
}
```

Respuestas relevantes: 400 para perfil/parámetros inválidos; 409 si ya existe ejecución activa para el perfil. Los errores no manejados de lanzamiento se convierten en `IllegalStateException`; el controller no define handler específico para ella.

Los GET operativos de estado, resumen y resultado existen, pero requieren `app.endpoints.operations.enabled=true`. El contrato inicial publicado por gateway contempla únicamente el POST de inicio y `/actuator/health`.

### Clasificación de la superficie REST

| Superficie | Clasificación | Habilitación | Uso esperado |
| --- | --- | --- | --- |
| `POST /api/v1/nominas/batch/start` | Contrato principal | Siempre cargado | Inicio productivo del batch |
| `/actuator/health` | Operacional | Según exposición del gateway | Health del servicio |
| GET de estado/resumen/resultado | Interno / operativo | `app.endpoints.operations.enabled=true` | Soporte y operación controlada |
| Controllers de diagnóstico | Diagnóstico interno | `app.diagnostics.enabled=true` | Pruebas técnicas controladas; no exponer en producción |
| Administración de metadata | Administrativo interno | `app.admin.enabled=true` | Operación administrativa autorizada |

## 6. Contratos y payloads

Todos los tokens siguientes se representan como `****`.

> **Nota contractual:** los nombres de elementos, namespaces, mayúsculas/minúsculas y grafías mostrados para cada operación Artikos corresponden a contratos específicos y deben respetarse literalmente. No deben normalizarse entre `NOMFACTERP`, `NOMFACTCONFIR` y `NOMFACTRES`.

### 6.1 NOMFACTERP

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <EjecutaTrx xmlns="AtkWs_DocExtractor">
      <token>****</token><msgCode>NOMFACTERP</msgCode>
      <msgFromAdress>PERFIL</msgFromAdress>
      <MsgCodFromAdress>****</MsgCodFromAdress>
      <msgToAdress>ARTIKOS</msgToAdress>
      <msgDateTime>dd/MM/yyyy HH:mm:ss</msgDateTime>
      <msgCodSis>SAF</msgCodSis><msgCallBack/><msgCodErp/>
      <msgCodExterno>VALOR_CONFIGURADO</msgCodExterno>
    </EjecutaTrx>
  </soap:Body>
</soap:Envelope>
```

La respuesta SOAP contiene el XML de nómina que el parser convierte a `Nomina`, `NominaHeader`, `DocumentoContable`, `Conciliacion` y `DistribucionContable`. Las muestras versionadas incluyen XML VIDA y GENERALES; no se reproduce aquí información de proveedores de dichas muestras.

### 6.2 NOMFACTCONFIR

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:atk="AtkWs_DocConnectorB2B">
  <soapenv:Body><atk:EjecutaTrx>
    <atk:Token>****</atk:Token><atk:MSgCode>NOMFACTCONFIR</atk:MSgCode>
    <atk:MsgFromAddres>PERFIL</atk:MsgFromAddres>
    <atk:MsgCodFromAddres>****</atk:MsgCodFromAddres>
    <atk:MsgToAddres>ARTIKOS</atk:MsgToAddres>
    <atk:MsgDateTime>dd/MM/yyyy HH:mm:ss</atk:MsgDateTime><atk:MsgCodSis>SAF</atk:MsgCodSis>
    <atk:MsgXmlDocument>&lt;Message&gt;...&lt;Respuesta&gt;&lt;NumeroNomina&gt;15960&lt;/NumeroNomina&gt;&lt;EstadoRespuesta&gt;0&lt;/EstadoRespuesta&gt;...&lt;/Message&gt;</atk:MsgXmlDocument>
    <atk:MsgNumber/>
  </atk:EjecutaTrx></soapenv:Body>
</soapenv:Envelope>
```

El XML interno se escapa al insertarse en `MsgXmlDocument`. `EstadoRespuesta` sólo admite 0 o 1; el flujo automático usa 0.

### 6.3 Procurement `post/document`

La ruta por defecto en properties es `/api/v1/document`, pero Azure App Configuration puede reemplazarla; el manifiesto dev versionado usa `/document` porque la base URL ya incorpora el prefijo del gateway. La URL efectiva siempre es concatenación normalizada de ambos valores.

```json
{
  "COD_TIP_DOCUMT": "CMP",
  "CMP": {
    "CMP_DOCUMT": {
      "COD_TIP_DOCUMT": "FEC", "COD_EMPRES": "001",
      "NUM_PERIODO": 2026, "NUM_RUT": 11111111,
      "NUM_DOCCMP": "DOC-SAN-001", "COD_SISTEM": "CM",
      "COD_CUENTA": "6131311000", "COD_TIP_CUENTA": "2",
      "COD_CONTBL": "1", "COD_MONEDA": "$",
      "FEC_EMIDCM": "2026-06-03", "GLS_DOCUMT": "FEC PROVEEDOR DEMO S.A.",
      "MTO_TOT_NTODIG": 10000, "MTO_TOT_EXNDIG": 0,
      "MTO_TOT_IVADIG": 1900, "MTO_TOT_DOCDIG": 11900,
      "NUM_FOL_DOCUMT": 1001, "CODIGO_REC_IVA": "U"
    },
    "CMP_DOCUMT_DET": [{
      "NUM_LIN_DOCCMP": 1, "COD_TIP_UNID": "UNI",
      "GRL_COD_ITEM": "ITEMSAN001", "COD_CCOSTO": "20001",
      "COD_CUENTA": "6131311000", "NUM_CANTDD": 1,
      "MTO_NETO": 10000, "PCT_IVA": 19, "MTO_IVACLC": 1900,
      "MTO_TOT_ITEM": 11900
    }],
    "CMP_DOCUMT_DET_RUT": {"CMP_NUM_RUT": 11111111, "NUM_RUT": 11111111, "A_IND_VIGE": "V"}
  },
  "HNR": null
}
```

El contrato completo incluye además fechas, tipo de cambio, descuentos, exento/afecto y montos unitarios. Las claves `CMP_DOCUMT`, `CMP_DOCUMT_DET` y `CMP_DOCUMT_DET_RUT` son secciones del payload y evidencian el modelo downstream; el repo no contiene DDL ni JPA para afirmar la estructura física de tablas documentales ASI con esos nombres.

### 6.4 NOMFACTRES

```xml
<Message>
  <MessageId><MsgCode>NOMFACTRES</MsgCode><MsgDesc>Actualizacion de carga de documentos</MsgDesc><MsgVersion>V2.0</MsgVersion>...</MessageId>
  <Respuesta>
    <Cabecera><NumeroNomina>900001</NumeroNomina><CantidadOK>1</CantidadOK><CantidadNOK>1</CantidadNOK><CantidadInformados>2</CantidadInformados></Cabecera>
    <Documentos>
      <Doc><DocFolio>DOC-SAN-001</DocFolio><DocRutProveedor>11111111-1</DocRutProveedor><DocTipoDoc>FEC</DocTipoDoc><Monto>11900</Monto><DocEstado>OK</DocEstado><DocDescEstado>Documento procesado correctamente</DocDescEstado></Doc>
      <Doc><DocFolio>DOC-SAN-002</DocFolio><DocRutProveedor>22222222-2</DocRutProveedor><DocTipoDoc>FEC</DocTipoDoc><Monto>5000</Monto><DocEstado>NOK</DocEstado><DocDescEstado>Documento rechazado funcionalmente por Procurement</DocDescEstado></Doc>
    </Documentos>
  </Respuesta>
</Message>
```

Este XML se transporta escapado dentro del mismo envelope connector mostrado para confirmación, usando `MSgCode=NOMFACTRES` y un token de resultado independiente.

## 7. Mapping Procurement / ASI

- `Tipo_ERP` determina el tipo de documento: `FEC/33`, `FCE/34`, `NDC/56`, `ECC/61`. Otro valor genera `ProcurementMappingException` (“Unsupported Artikos Tipo_ERP…”).
- `Msg_To` determina empresa: VIDA `001` y GENERALES `002` según los alias aceptados por el mapper.
- `USO_IVA` admite `U`, `R`, `N`; vacío usa `U`.
- Por distribución se consulta `GRL_MAE_ITEM_DET` con empresa, cuenta, sistema (`CM` por defecto) e impuesto. Debe existir una única homologación.
- Se valida `GRL_MAE_ITEM` activo para empresa, período e ítem.
- La homologación aporta ítem, unidad, tipo de cuenta, contable, sistema, período y moneda.
- `Quantity` alimenta cantidad; si no existe se usa 1.

## 8. Datasources y persistencia

La configuración declara dos pools Hikari independientes:

| Datasource | Beans | Uso |
| --- | --- | --- |
| APP | `appDataSource`, `appEntityManagerFactory`, `appTransactionManager` (`@Primary`) | `CONTROL_NOMINA`, `GRL_MAE_ITEM`, `GRL_MAE_ITEM_DET` y transacción del step |
| BATCH | `batchDataSource`, `batchTransactionManager` (`@BatchDataSource`) | `JobRepository`, `JobExplorer` y tablas `BATCH_*` |

Ambos pueden apuntar a la misma instancia física o a esquemas/credenciales diferentes. `spring.batch.jdbc.initialize-schema=never`: las tablas deben preexistir. El prefijo es configurable, por ejemplo `BATCH_` o un prefijo calificado por esquema. El repo incluye `V000__create_spring_batch_metadata.sql`.

### `CONTROL_NOMINA`

Clave primaria compuesta: `JOB_EXECUTION_ID`, `NUMERO_NOMINA`. Columnas: totales de documentos OK/NOK, conciliaciones y distribuciones; `STATUS`; timestamps; `ERROR_MESSAGE` de 500 caracteres; `COD_EMPRES CHAR(3)`. Estados permitidos: `PROCESSING`, `OK`, `NOK`, `ERROR`. Hay índices por job, estado y número de nómina.

`COD_EMPRES` representa la empresa asociada a la nómina: para el perfil VIDA corresponde a `001` y para GENERALES a `002`. El campo fue incorporado mediante la migración separada `V002__add_cod_empres_to_control_nomina.sql` para mantener compatibilidad con instalaciones de una versión anterior de la aplicación. La columna no posee restricción `NOT NULL`; por ello puede permanecer temporalmente nula si la migración de base de datos se despliega antes que la versión de la aplicación que comienza a informarla. El script consolidado de creación `V001__create_control_nomina.sql` versionado actualmente también contiene la columna para instalaciones nuevas.

### Maestros ASI

`GRL_MAE_ITEM_DET` se mapea con clave compuesta por empresa, período, sistema, moneda, cuenta, impuesto e ítem; expone unidad, tipo de cuenta, contable y vigencia. `GRL_MAE_ITEM` tiene clave empresa, período e ítem, y vigencia. El adapter sólo lee ambas tablas.

## 9. Configuración

### Archivos Spring

- `application.properties`: configuración común, datasources, JPA/Batch, límites, toggles, timeouts, mapping Procurement y matrices Artikos VIDA/GENERALES.
- `application-qa.properties`: activa validación estricta por defecto.
- `application-prod.properties`: validación estricta y niveles de log más restrictivos.
- `application-local.example.properties`: plantilla local sin secretos; el build excluye `application-local.properties`.
- `bootstrap.properties`: fuera de `local`, conecta Azure App Configuration usando endpoint, label, filtro y prefijo a recortar.
- `bootstrap-local.properties`: soporte del perfil local sin carga remota.

### Azure App Configuration y Key Vault

Los JSON `azure/app-config/appcfg.dev.json`, `appcfg.pre.json` y `appcfg.prod.json` definen claves por ambiente. Valores no sensibles se almacenan como configuración. Datasource passwords/usuarios/URLs y tokens Artikos se representan como referencias Key Vault con content type de Key Vault; no como secretos en claro. La identidad/credenciales Azure llegan al pod desde un Secret Kubernetes (`envFrom`).

Los perfiles funcionales VIDA y GENERALES tienen tres configuraciones separadas: consumo (`NOMFACTERP`), respuesta (`NOMFACTCONFIR`) y resultado (`NOMFACTRES`), cada una con token y metadatos de mensaje. No deben confundirse con los perfiles Spring `qa`, `prod` o `local`.

### Variables de entorno principales

| Grupo | Variables |
| --- | --- |
| Azure | `URL_APP_CONFIGURATION`, `APPCFG_LABEL`, `APPCFG_KEY_FILTER`, `APPCFG_TRIM_PREFIX` |
| APP DB | `APP_DATASOURCE_URL`, `APP_DATASOURCE_USERNAME`, `APP_DATASOURCE_PASSWORD`, driver/pool |
| BATCH DB | equivalentes `BATCH_DATASOURCE_*`, `SPRING_BATCH_JDBC_TABLE_PREFIX` |
| Ejecución | `ATK_BATCH_REAL_CHUNK_SIZE`, `ATK_BATCH_DEFAULT_MAX_NOMINAS`, `ATK_BATCH_MAX_NOMINAS_PER_RUN` |
| Artikos | URLs, `ARTIKOS_SOURCE_MODE`, toggles confirm/result, timeouts/retry y variables por perfil/operación |
| Procurement | `PROCUREMENT_BASE_URL`, `PROCUREMENT_DOCUMENT_PATH`, toggles, timeouts y constantes de mapping |
| Exposición | `APP_DIAGNOSTICS_ENABLED`, `APP_ADMIN_ENABLED`, `APP_ENDPOINTS_OPERATIONS_ENABLED` |

Los valores sensibles deben inyectarse por Key Vault o mecanismo corporativo equivalente y nunca registrarse. Los builders poseen enmascarado de token, aunque debe evitarse habilitar payload logging en producción.

## 10. Manejo de errores

| Caso | Resultado |
| --- | --- |
| Artikos sin nóminas | Fin normal, sin fila nueva |
| Falla HTTP/timeout/parsing en NOMFACTERP | job `FAILED`; normalmente no hay número para control |
| Rechazo NOMFACTCONFIR | `CONTROL_NOMINA=ERROR`, job `FAILED` |
| Procurement `statusCode` funcional distinto de 0 y no duplicado | El documento queda `NOK`; continúa con los documentos siguientes, se informa `NOMFACTRES` y la nómina queda `NOK` |
| Mapping de un documento hacia Procurement | Se clasifica `PROCUREMENT_MAPPING_ERROR`, pero `ProcurementDocumentProcessingService` lo captura por documento; ese documento queda `NOK`, continúa el loop y se informa en `NOMFACTRES` |
| Procurement timeout, conexión, 5xx, body no parseable o indicios SQL/JDBC/ORA/constraint durante un documento | Se clasifica `PROCUREMENT_TECHNICAL_ERROR`, se captura en la misma frontera documental; ese documento queda `NOK`, continúa el loop y se informa en `NOMFACTRES` |
| Duplicado reconocido | documento OK idempotente |
| Error técnico crítico o error de nómina fuera de la frontera documental | Puede dejar `CONTROL_NOMINA=ERROR`, fallar el job y evitar `NOMFACTRES` cuando no existe un resultado confiable |
| Rechazo/falla NOMFACTRES | `ERROR`, job `FAILED` |
| Error persistiendo control | `ORACLE_CONTROL_ERROR`; job `FAILED`; el estado puede quedar incierto |

Los errores se clasifican mediante `IntegrationErrorType`. El texto persistido se compacta/trunca. Para un rechazo funcional, `ProcurementResultMapper` crea un `ResultadoDocumento` NOK. Para una excepción técnica o de mapping individual, `ProcurementIntegrationService` la convierte en `ArtikosIntegrationException` y `ProcurementDocumentProcessingService` la captura dentro del `for`, agrega también un `ResultadoDocumento` NOK y continúa. En ambos casos `NominaProcessingService` calcula el resumen: `DocEstado=NOK` y `DocDescEstado` contienen el motivo sanitizado, `CONTROL_NOMINA` finaliza `NOK` si Artikos acepta el resultado y se envía `NOMFACTRES` con los totales OK/NOK.

La detención ocurre cuando la excepción queda fuera de esa frontera por documento o cuando no puede construirse/enviarse un resultado confiable de nómina. Entre estos casos están el rechazo de `NOMFACTCONFIR`, una falla general de procesamiento, un error de control Oracle o el rechazo/fallo de `NOMFACTRES`. Según el tipo y la disponibilidad del número de nómina, `NominaErrorPolicyService` puede ordenar el marcado de `CONTROL_NOMINA=ERROR`; el job termina `FAILED` y puede no existir `NOMFACTRES` informado.

## 11. Idempotencia y reprocesamiento

La idempotencia tiene dos niveles:

1. Documento Procurement: `statusCode=-20` se trata como OK idempotente. También existe respaldo por mensajes conocidos (`ya existe`, `duplicate`, `duplicado`, `unique constraint`, `ORA-00001`).
2. Nómina: antes de confirmar/procesar, se busca el último `CONTROL_NOMINA` por `NUMERO_NOMINA`. Si está `OK`, no se reenvían documentos a Procurement; se genera un resultado “already OK”. Estados `NOK`, `ERROR` o `PROCESSING` permiten reproceso.

Limitación verificable: la consulta de reproceso usa número de nómina y no filtra por `COD_EMPRES/profile`; si los números no fueran globalmente únicos podría existir ambigüedad. Además, la clave física de `CONTROL_NOMINA` incluye job+nómina, por lo que cada ejecución conserva su propia fila.

## 12. Despliegue

El `Dockerfile` es multi-stage: compila con Maven/JDK 17 ejecutando `mvn clean package -DskipTests`, copia el JAR a Amazon Corretto 17 Alpine, expone 8080 y ejecuta `java -jar app.jar`.

`.gitlab-ci.yml` declara etapas test, build, deploy y cleanup e incluye componentes corporativos para lint/build/deploy Docker, review apps, SonarQube, despliegue de Azure App Configuration y creación de secretos Key Vault. El deploy apunta al namespace Kubernetes `artikos` y a un repositorio IaC/Flux indicado en el pipeline.

Kubernetes usa Kustomize. La base contiene `Deployment` y `Service ClusterIP` (80 -> 8080); solicita 64m/64Mi y limita 300m/1024Mi, usa AppArmor runtime/default e image pull secret. El overlay dev agrega ingress, afinidad, réplicas, límites y variables Azure. La imagen concreta y reconciliación final se administran en la cadena CI/IaC/Flux.

## 13. Estado de validación de la solución

La solución completó su ciclo de construcción, pruebas, preproducción y paso a producción. Durante las validaciones previas al go-live se confirmó, entre otros aspectos, que:

- La aplicación levanta correctamente en Kubernetes.
- Azure App Configuration y Key Vault resuelven la configuración y los secretos requeridos.
- Los datasources APP y BATCH conectan correctamente.
- Spring Batch inicia ejecuciones bajo demanda desde el endpoint REST.
- Artikos responde correctamente a `NOMFACTERP` y entrega nóminas.
- `CONTROL_NOMINA` registra `PROCESSING` y los estados finales.
- Artikos responde correctamente a `NOMFACTCONFIR`.
- El lookup ASI contra `GRL_MAE_ITEM_DET` y `GRL_MAE_ITEM` funciona cuando existen los permisos y datos maestros requeridos.
- Procurement es alcanzable mediante la ruta configurada.
- El problema de certificados/truststore observado durante las pruebas fue resuelto.
- Artikos responde correctamente a `NOMFACTRES`.

El servicio fue posteriormente validado y desplegado en producción.

## 14. Consideraciones para producción y limitaciones conocidas

- Mantener `artikos.source.mode=remote`, confirmación/resultado y ambas capas Procurement habilitadas sólo tras validar endpoints y contratos del ambiente.
- Deshabilitar diagnóstico, administración, endpoints operativos no publicados, Swagger y API docs; proteger la exposición con el gateway corporativo.
- Crear y otorgar permisos mínimos sobre `CONTROL_NOMINA`, `GRL_MAE_ITEM`, `GRL_MAE_ITEM_DET` y `BATCH_*`; confirmar el schema/prefijo efectivo.
- Validar certificados y truststore de Artikos, Procurement, Azure App Configuration y Key Vault.
- Mantener secretos sólo en Key Vault/Secret administrado; desactivar logging de payloads o conservarlo sanitizado.
- Dimensionar pools, timeouts, retry y `maxNominas`; el executor admite dos ejecuciones, pero cada perfil sólo una.
- Monitorear `actuator/health`, estados `BATCH_*`, `CONTROL_NOMINA`, latencia SOAP/REST y acumulación de metadata.
- Ejecutar pruebas integrales con una nómina en estados Artikos válidos antes de cualquier cambio relevante de ambiente o integración.

## 15. Diagnóstico técnico

Esta sección resume síntomas técnicos frecuentes. Para procedimientos operativos completos, recuperación, reintentos seguros y escalamiento, consultar `docs/runbook.md` y `docs/support-guide.md`.

### PKIX certificate path

Síntoma: `PKIX path building failed` al llamar HTTPS. Causa probable: CA/cadena no confiable en el truststore del JRE del contenedor. Verificar cadena completa, hostname y proxy; incorporar la CA por el mecanismo corporativo y reconstruir la imagen. No deshabilitar validación TLS.

### ORA-00942: table or view does not exist

Confirmar usuario/schema del datasource correspondiente, existencia de `CONTROL_NOMINA`, maestros o `BATCH_*`, grants directos y `SPRING_BATCH_JDBC_TABLE_PREFIX`. Si Batch vive en otro esquema, usar el prefijo calificado configurado.

### ORA-02291: integrity constraint violated — parent key not found

El código clasifica textos ORA/constraint como error técnico Procurement. Revisar en Procurement qué registro maestro/padre ASI falta y contrastar empresa, período, ítem, cuenta, proveedor y códigos homologados. El adapter no puede reparar esa integridad referencial.

### HTTP 404 Procurement por path incorrecto

Inspeccionar la composición `PROCUREMENT_BASE_URL + PROCUREMENT_DOCUMENT_PATH`. No duplicar ni omitir el prefijo del gateway: el default de aplicación es `/api/v1/document`, mientras el App Configuration dev versionado usa una base con prefijo y `/document`. Validar la URL efectiva sin exponer credenciales.

### Unsupported Tipo_ERP

El XML contiene un tipo fuera de `FEC/33`, `FCE/34`, `NDC/56`, `ECC/61`. Corregir el dato/contrato o ampliar el mapper con aprobación funcional. Actualmente se clasifica como `PROCUREMENT_MAPPING_ERROR`; dentro del procesamiento individual la excepción se captura, el documento queda `NOK`, continúa la nómina y el detalle se incluye en `NOMFACTRES`.

### Ambiguous GRL_MAE_ITEM_DET mapping

La búsqueda devolvió más de una homologación para empresa+cuenta+sistema+impuesto. Revisar duplicidad/vigencia y claves (período, moneda, ítem) en ASI. El servicio rechaza la selección arbitraria y genera un error de mapping para ese documento; la frontera documental lo convierte en `NOK` y continúa con los documentos siguientes.

## 16. Evidencia revisada

Se revisó el inventario completo versionado, excluyendo artefactos generados (`target`, logs y metadata Git), y se profundizó en:

- `pom.xml`, `README.md`, `Dockerfile`, `.gitlab-ci.yml`, `.dockerignore`, Sonar y scripts auxiliares.
- Todo `src/main/java`: aplicación, controllers/DTO, batch config/readers/processors/writers, fuentes Artikos, dominio, services, cliente/builders/parsers SOAP, Procurement client/config/DTO/mappers/services/lookups, repositories, datasource/health/logging.
- `src/main/resources`: properties comunes y por perfil, bootstrap, logging, migraciones Oracle y rollback.
- `src/test`: pruebas unitarias/integración, properties, schema H2 y muestras XML Artikos sanitizadas para comprender contratos y casos límite.
- `azure/app-config`: definiciones dev/pre/prod, tratando referencias Key Vault como sensibles y sin reproducir URIs/valores identificadores.
- `kubernetes/base` y `kubernetes/overlays/dev`: deployment, service, ingress, kustomization y patches.
- Documentos `docs/`, ADR, runbooks, guías de mapping/integración/error y evidencias sanitizadas.

## 17. Documentación relacionada

Código fuente de mayor relevancia: `NominaBatchJobConfig`, `ArtikosNominaItemReader`, `ArtikosNominaItemProcessor`, `ArtikosNominaResultItemWriter`, `NominaProcessingService`, `ProcurementClient`, `ProcurementDocumentMapper`, `ProcurementMappingLookupService`, `ControlNominaService`, `DataSourceConfig` y las migraciones `V000`–`V002`.

Documentación complementaria existente:

- `README.md` — portada del producto y punto de entrada general.
- `docs/onboarding.md` — guía inicial y mapa documental para nuevos mantenedores.
- `docs/architecture.md` — arquitectura as-built de alto nivel.
- `docs/batch-flow.md` — detalle del flujo Spring Batch.
- `docs/endpoints.md` — contratos REST.
- `docs/error-handling.md` — clasificación y manejo de errores.
- `docs/procurement-mapping.md` — reglas de mapping hacia Procurement.
- `docs/procurement-integration.md` — integración REST Procurement.
- `docs/environments-and-dependencies.md` — ambientes, configuración y dependencias externas.
- `docs/infra-delivery.md` — infraestructura y artefactos de despliegue.
- `docs/runbook.md` — operación normal del servicio.
- `docs/support-guide.md` — troubleshooting y soporte productivo.
- `docs/release-and-deployment.md` — ciclo de cambio, release y despliegue GitHub/GitLab cliente.
- `docs/technical-maintenance.md` — mantenimiento técnico, Spring Batch, Oracle y metadata.
- `docs/handover-checklist.md` — checklist final de continuidad y handover.
- ADR-003 — decisiones arquitectónicas relacionadas con el batch y persistencia.

## 18. Glosario

| Término | Definición |
| --- | --- |
| Adapter | Componente que conecta sistemas con contratos distintos y transforma mensajes entre ellos. En este proyecto integra Artikos con Procurement/ASI. |
| Artikos | Sistema externo que publica las nóminas contables y recibe su confirmación y resultado de procesamiento. |
| Procurement | Servicio REST receptor de los documentos transformados; procesa el contrato CMP y gestiona su incorporación en ASI. |
| ASI | Sistema y esquema de datos corporativo consultado para homologaciones y utilizado por Procurement para la información documental. |
| Spring Batch | Framework que organiza y registra la ejecución controlada de procesos por lotes. |
| Job | Definición completa de un proceso Spring Batch. En esta aplicación corresponde a `nominaDocumentosContablesJob`. |
| Step | Etapa ejecutable dentro de un job; combina lectura, procesamiento y escritura. |
| JobExecution | Instancia concreta de ejecución de un job, identificada por `jobExecutionId`, con estado, parámetros y tiempos propios. |
| Reader | Componente del step que obtiene el siguiente ítem; aquí consulta o lee una nómina Artikos. |
| Processor | Componente que aplica reglas y transforma el ítem leído en un resultado de nómina. |
| Writer | Componente que persiste o comunica el resultado; aquí envía `NOMFACTRES` y actualiza el control. |
| Datasource APP | Conexión Oracle principal usada por JPA para `CONTROL_NOMINA` y los maestros ASI. |
| Datasource BATCH | Conexión Oracle destinada a la metadata técnica `BATCH_*` de Spring Batch. |
| `CONTROL_NOMINA` | Tabla de control funcional que registra por ejecución y nómina su empresa, estado, totales y error asociado. |
| `NOMFACTERP` | Operación SOAP utilizada para solicitar a Artikos la siguiente nómina disponible. |
| `NOMFACTCONFIR` | Operación SOAP utilizada para confirmar a Artikos la recepción de una nómina. |
| `NOMFACTRES` | Operación y mensaje SOAP utilizados para informar a Artikos el resultado consolidado OK/NOK de la nómina. |
| Idempotencia | Propiedad que evita efectos duplicados cuando se repite una operación ya procesada. |
| Mapping | Conversión y homologación de campos entre el modelo Artikos y el contrato esperado por Procurement. |
| Truststore | Almacén de certificados de confianza usado por Java para validar conexiones TLS/HTTPS. |
| Key Vault | Servicio Azure utilizado para custodiar secretos y exponerlos mediante referencias controladas. |
| Azure App Configuration | Servicio Azure que centraliza propiedades de configuración por ambiente y referencias a secretos. |
| JPA | Especificación Java para mapear entidades y operaciones de persistencia sobre una base de datos relacional. |
| Hibernate | Implementación de JPA utilizada por Spring Data JPA para ejecutar el acceso ORM a Oracle. |
