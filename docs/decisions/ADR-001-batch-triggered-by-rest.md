# ADR-001: Batch disparado por REST

## Estado

Aceptada

## Contexto

La aplicacion requiere ejecutar procesamiento batch bajo demanda para validar flujos de nomina, parametrizacion y respuesta operacional sin depender inicialmente de un scheduler externo.

## Decision

El procesamiento batch sera disparado mediante endpoints REST versionados bajo `/api/v1`. La API delegara la ejecucion a servicios de aplicacion, que a su vez usaran Spring Batch para lanzar jobs y consultar estado.

## Consecuencias

- Permite probar el flujo desde herramientas HTTP y automatizaciones simples.
- Facilita documentar contratos con OpenAPI desde etapas tempranas.
- Mantiene abierta la posibilidad de agregar scheduler en un sprint posterior.
- Requiere controlar idempotencia, concurrencia y validacion de parametros antes de pasar a produccion.
