# Onboarding — Artikos Integration

## 1. Objetivo de esta guía

Esta guía es el punto de entrada recomendado para desarrolladores y mantenedores que comiencen a trabajar con `artikos-integration`.

El objetivo no es reemplazar la documentación técnica existente, sino entregar una visión inicial del sistema y orientar hacia el documento correcto según la tarea que se necesite realizar.

`artikos-integration` se encuentra actualmente en producción y debe considerarse un sistema bajo continuidad operativa.

## 2. Qué hace el sistema

`atk-nomina-batch` es un microservicio desarrollado con Java, Spring Boot y Spring Batch encargado de integrar nóminas de documentos contables entre Artikos y Procurement / ASI.

A alto nivel, el servicio:

1. consulta nóminas disponibles en Artikos;
2. confirma su recepción;
3. procesa los documentos contenidos en la nómina;
4. transforma cada documento al contrato esperado por Procurement;
5. envía los documentos a Procurement;
6. construye el resultado consolidado de la nómina;
7. informa dicho resultado nuevamente a Artikos;
8. registra información funcional y técnica de la ejecución en Oracle.

Los perfiles funcionales soportados son:

- `VIDA`
- `GENERALES`

## 3. Sistemas involucrados

```text
                        +----------------+
                        |    Artikos     |
                        |                |
                        | NOMFACTERP     |
                        | NOMFACTCONFIR  |
                        | NOMFACTRES     |
                        +-------+--------+
                                |
                                | SOAP
                                |
                                v
                    +-----------------------+
                    |   atk-nomina-batch    |
                    |                       |
                    | Spring Boot           |
                    | Spring Batch          |
                    +-----+------------+----+
                          |            |
                     REST |            | JDBC
                          |            |
                          v            v
                 +-------------+   +---------+
                 | Procurement |   | Oracle  |
                 |             |   |         |
                 | POST        |   | ASI     |
                 | /document   |   | Batch   |
                 +------+------+   +---------+
                        |
                        v
                       ASI
```

El adapter **no inserta directamente los documentos contables en ASI**.

La aplicación construye y envía el contrato documental a Procurement. La persistencia final del documento en ASI es responsabilidad de Procurement.

El adapter sí accede directamente a Oracle para:

- `CONTROL_NOMINA`;
- `GRL_MAE_ITEM`;
- `GRL_MAE_ITEM_DET`;
- tablas de metadata Spring Batch `BATCH_*`.

Para mayor detalle revisar:

- [`architecture.md`](architecture.md)
- [`documentacion-tecnica-artikos-asi.md`](documentacion-tecnica-artikos-asi.md)

## 4. Cómo se inicia una ejecución

El contrato productivo principal es:

```http
POST /api/v1/nominas/batch/start
```

Ejemplo:

```json
{
  "profile": "GENERALES",
  "dryRun": false
}
```

La llamada inicia un job Spring Batch de forma asíncrona y retorna un `jobExecutionId`.

Ese identificador es una de las principales referencias para realizar seguimiento o diagnóstico posteriormente.

El batch continúa consultando Artikos hasta que:

- Artikos informa que no existen más nóminas disponibles; o
- se alcanza el límite operativo `maxNominas`.

`maxNominas` es un mecanismo de seguridad operacional y no representa la condición funcional normal de término.

## 5. Flujo funcional resumido

```text
POST /api/v1/nominas/batch/start
                |
                v
        Inicia Spring Batch
                |
                v
         Artikos NOMFACTERP
                |
                v
        ¿Existe nómina?
          |           |
         no          sí
          |           |
          v           v
       Termina   CONTROL_NOMINA
                  PROCESSING
                       |
                       v
               NOMFACTCONFIR
                       |
                       v
               Procesar documentos
                       |
                       v
                 Procurement
                 POST /document
                       |
                       v
                Crear NOMFACTRES
                       |
                       v
               Enviar a Artikos
                       |
                       v
                CONTROL_NOMINA
                   OK / NOK
```

Consultar el flujo completo en:

[`batch-flow.md`](batch-flow.md)

## 6. Componentes que debe conocer un mantenedor

No es necesario dominar todos estos componentes antes de comenzar, pero sí comprender qué responsabilidad tiene cada uno dentro de la solución.

| Componente | Uso en el proyecto |
|---|---|
| Java 17 | Runtime de la aplicación |
| Spring Boot | Framework principal |
| Spring Batch | Orquestación del procesamiento de nóminas |
| SOAP | Comunicación con Artikos |
| REST | Comunicación con Procurement |
| Oracle | Persistencia funcional, metadata batch y lookup ASI |
| `CONTROL_NOMINA` | Estado funcional de las nóminas |
| `BATCH_*` | Metadata técnica de Spring Batch |
| Maven | Build y dependencias |
| Docker | Empaquetado de la aplicación |
| GitLab CI/CD | Pipeline corporativo |
| Kubernetes | Ejecución de la aplicación |
| Azure App Configuration | Configuración por ambiente |
| Azure Key Vault | Secretos |
| CONC / Kong | Gateway corporativo |
| Logs / observabilidad | Diagnóstico y soporte |

## 7. Cómo orientarse dentro del repositorio

La documentación está separada por responsabilidad.

| Necesito... | Consultar |
|---|---|
| Entender rápidamente el proyecto | `README.md` |
| Comenzar a mantenerlo | `docs/onboarding.md` |
| Entender arquitectura | `docs/architecture.md` |
| Revisar el diseño técnico completo | `docs/documentacion-tecnica-artikos-asi.md` |
| Entender el job Spring Batch | `docs/batch-flow.md` |
| Entender el mapping hacia Procurement | `docs/procurement-mapping.md` |
| Entender homologaciones ASI | `docs/asi-lookup.md` |
| Operar el servicio | `docs/runbook.md` |
| Diagnosticar errores conocidos | `docs/support-guide.md` |
| Consultar Oracle | `docs/sql-queries.md` |
| Mantener Spring Batch, Oracle y metadata | `docs/technical-maintenance.md` |
| Entender ambientes y dependencias externas | `docs/environments-and-dependencies.md` |
| Revisar infraestructura | `docs/infra-delivery.md` |
| Revisar exposición mediante gateway | `docs/gateway-endpoints.md` |
| Ejecutar pruebas locales E2E | `docs/local-e2e-testing.md` |
| Ejecutar replay de una nómina | `docs/artikos-replay-local.md` |
| Validar Artikos remoto | `docs/artikos-remote-e2e.md` |
| Entender decisiones arquitectónicas | `docs/decisions/` |

## 8. Ruta de lectura recomendada

Para una persona que recibe el proyecto por primera vez se recomienda el siguiente orden.

### Primera lectura

1. `README.md`
2. `docs/onboarding.md`
3. `docs/architecture.md`

Al completar estos documentos debería ser posible comprender qué hace el sistema y cuáles son sus principales componentes.

### Profundización técnica

4. `docs/documentacion-tecnica-artikos-asi.md`
5. `docs/batch-flow.md`
6. `docs/procurement-mapping.md`
7. `docs/asi-lookup.md`

### Operación, soporte y mantenimiento

8. `docs/runbook.md`
9. `docs/support-guide.md`
10. `docs/sql-queries.md`
11. `docs/technical-maintenance.md`

### Infraestructura y despliegue

12. `docs/environments-and-dependencies.md`
13. `docs/infra-delivery.md`
14. `docs/gateway-endpoints.md`
15. `docs/release-and-deployment.md`

No es necesario estudiar toda la documentación antes de realizar una intervención. El mapa documental anterior puede utilizarse para profundizar únicamente en el área requerida.

## 9. Primer levantamiento local

Antes de trabajar sobre la aplicación verificar como mínimo:

```bash
java -version
mvn -version
```

El proyecto utiliza Java 17.

Ejecutar compilación y pruebas:

```bash
mvn clean verify
```

Para construir el artefacto:

```bash
mvn clean package
```

La ejecución real requiere configuración de servicios externos y Oracle.

No utilizar credenciales productivas en archivos locales versionados.

Para pruebas sin consumir Artikos remoto existe el modo:

```properties
artikos.source.mode=local-xml
```

Consultar antes de ejecutar:

- [`local-e2e-testing.md`](local-e2e-testing.md)
- [`artikos-replay-local.md`](artikos-replay-local.md)

## 10. Primer diagnóstico ante un incidente

Ante una falla, evitar comenzar directamente revisando código.

Seguir inicialmente esta ruta:

```text
¿La aplicación está disponible?
            |
            v
     /actuator/health
            |
            v
 Identificar jobExecutionId
            |
            v
       Revisar logs
            |
            v
     CONTROL_NOMINA
            |
            v
 BATCH_JOB_EXECUTION
            |
            v
 Identificar operación fallida
            |
            +--> NOMFACTERP
            +--> NOMFACTCONFIR
            +--> Procurement
            +--> NOMFACTRES
            +--> Oracle
            +--> procesamiento interno
            |
            v
     support-guide.md
```

Datos útiles para cualquier análisis:

- `jobExecutionId`;
- `profile`;
- `numeroNomina`;
- `operation`;
- estado Spring Batch;
- estado `CONTROL_NOMINA`.

Consultar:

- [`runbook.md`](runbook.md)
- [`support-guide.md`](support-guide.md)
- [`sql-queries.md`](sql-queries.md)

## 11. Estados que conviene conocer

### Spring Batch

- `STARTING`
- `STARTED`
- `COMPLETED`
- `FAILED`
- `STOPPING`
- `STOPPED`

### CONTROL_NOMINA

- `PROCESSING`
- `OK`
- `NOK`
- `ERROR`

Un job Spring Batch `COMPLETED` y una nómina `NOK` no representan necesariamente una contradicción.

Spring Batch informa el resultado técnico de la ejecución, mientras que `CONTROL_NOMINA` representa el resultado funcional de cada nómina.

## 12. Reglas críticas de operación

### No reenviar una nómina sin conocer su estado

Las operaciones Artikos poseen estados funcionales asociados.

Antes de reintentar manualmente una nómina debe verificarse su situación en Artikos y el resultado de la ejecución anterior.

### No eliminar metadata Batch manualmente

Las tablas `BATCH_*` contienen metadata necesaria para Spring Batch.

Existe un mecanismo controlado de purga documentado en el runbook.

### No modificar CONTROL_NOMINA como mecanismo normal de recuperación

`CONTROL_NOMINA` representa trazabilidad funcional.

Una modificación manual puede ocultar el estado real de un procesamiento.

### No asumir éxito por HTTP 2xx

Una comunicación HTTP técnicamente exitosa puede contener un rechazo funcional del sistema externo.

Siempre revisar el contrato de respuesta.

### No modificar mappings sin pruebas

Los mappings Artikos → Procurement tienen impacto contable.

Toda modificación debe contar como mínimo con:

- Issue;
- prueba asociada;
- Pull Request;
- validación del payload resultante.

### No habilitar endpoints administrativos permanentemente

Los endpoints administrativos, diagnósticos y operativos GET están deshabilitados por defecto en producción.

Su habilitación debe ser temporal y autorizada.

### No versionar secretos

Nunca almacenar en Git:

- passwords;
- tokens Artikos;
- credenciales Oracle;
- secretos de Procurement;
- connection strings sensibles;
- archivos locales con credenciales.

## 13. Cómo debe realizarse un cambio

`artikos-integration` debe mantenerse utilizando cambios trazables.

Flujo esperado:

```text
Requerimiento / incidente
          |
          v
      GitHub Issue
          |
          v
        Branch
          |
          v
    Implementación
          |
          v
        Tests
          |
          v
    Pull Request
          |
          v
       Revisión
          |
          v
        Merge
          |
          v
      QA / PRE
          |
          v
        PROD
          |
          v
 Validación post-deploy
```

No realizar modificaciones directas en `main`.

El proceso detallado se encuentra en [`release-and-deployment.md`](release-and-deployment.md).

## 14. Decisiones arquitectónicas

Las decisiones relevantes que expliquen por qué el sistema fue construido de determinada manera deben consultarse en:

```text
docs/decisions/
```

Antes de cambiar un comportamiento estructural revisar los ADR existentes.

Una decisión histórica puede representar una restricción técnica o funcional que no sea evidente únicamente leyendo el código.

## 15. Por dónde empezar según la tarea

### Tengo que corregir un mapping

Revisar:

1. `procurement-mapping.md`
2. modelos Artikos;
3. mapper Procurement;
4. pruebas del mapper.

### Falló una ejecución productiva

Revisar:

1. `runbook.md`
2. `support-guide.md`
3. `sql-queries.md`
4. logs mediante `jobExecutionId`.

### Tengo que entender una nómina

Revisar:

1. `batch-flow.md`
2. `documentacion-tecnica-artikos-asi.md`.

### Tengo que mantener Spring Batch u Oracle

Revisar:

1. `technical-maintenance.md`;
2. `sql-queries.md`;
3. `batch-flow.md`;
4. `environments-and-dependencies.md` cuando aplique configuración por ambiente.

### Tengo que modificar integración Artikos

Revisar:

1. arquitectura;
2. documentación técnica;
3. ADR de estados Artikos;
4. pruebas E2E y replay local.

### Tengo que modificar Procurement

Revisar:

1. `procurement-mapping.md`;
2. `asi-lookup.md`;
3. documentación técnica;
4. pruebas de integración/mapping.

### Tengo que revisar configuración o una dependencia externa

Revisar:

1. `environments-and-dependencies.md`;
2. `infra-delivery.md`;
3. `support-guide.md` cuando exista una falla concreta.

### Tengo que desplegar un cambio

Revisar:

1. `release-and-deployment.md`;
2. pipeline;
3. documentación de infraestructura;
4. checklist correspondiente al ambiente.

## 16. Principio de continuidad

El repositorio debe considerarse la fuente principal de conocimiento técnico del servicio.

Cuando durante operación o mantenimiento se descubra información necesaria para comprender, operar o recuperar el sistema, dicha información debe incorporarse a la documentación correspondiente mediante el mismo flujo de Issue y Pull Request utilizado para el código.

El objetivo es minimizar la dependencia de conocimiento individual o no documentado.
