# Sprint Plan

## Sprint 0 - Bootstrap

Objetivo: dejar el repositorio compilando con Spring Boot 3, Java 17, Spring Batch, H2 y documentacion minima.

Entregables:

- Proyecto Maven base.
- Dependencias iniciales.
- Estructura de paquetes.
- Configuracion `application.properties`.
- Endpoint `GET /api/v1/health`.
- Documentacion inicial.
- Pruebas con `mvn clean test`.

## Sprint 1 - REST Batch Launcher

Objetivo: incorporar endpoint REST para solicitar la ejecucion de jobs batch.

Entregables esperados:

- DTOs de request/response.
- Servicio de lanzamiento de jobs.
- Validaciones de entrada.
- Manejo inicial de errores.

## Sprint 2 - XML Parser

Objetivo: parsear archivo SOAP/XML de nomina local desde classpath.

Entregables esperados:

- Modelo de dominio inicial.
- Reader o servicio de lectura XML.
- Pruebas unitarias de parsing.

## Sprint 3 - Chunk Processing

Objetivo: configurar job Spring Batch con reader, processor y writer en chunks.

Entregables esperados:

- Job y step configurados.
- Chunk size configurable.
- Simulacion controlada por propiedades.

## Sprint 4 - Observabilidad y resumen

Objetivo: exponer estado y resultados de ejecuciones batch.

Entregables esperados:

- Resumen de ejecucion.
- Metricas/actuator basicas.
- Documentacion de operacion.
