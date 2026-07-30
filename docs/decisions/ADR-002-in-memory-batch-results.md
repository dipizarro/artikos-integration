# ADR-002: Resultados batch en memoria

## Estado

Aceptada

## Contexto

La aplicacion necesita exponer un resumen de procesamiento masivo simulado sin incorporar todavia una base de datos propia para resultados funcionales. Spring Batch mantiene su metadata tecnica, pero los totales de nominas, documentos, conciliaciones, distribuciones y XML NOMFACTRES generados son informacion de salida operacional.

## Decision

Los resultados funcionales del procesamiento se guardaran temporalmente en un store en memoria, indexado por `jobExecutionId` y `numeroNomina`. El writer agrega resultados por chunk y el job limpia el store al iniciar una nueva ejecucion.

## Consecuencias

- La implementacion se mantiene local y simple.
- El endpoint de summary puede consultar resultados sin crear tablas propias.
- El endpoint de resultado por nomina puede devolver el NOMFACTRES generado para una nomina especifica.
- Los resultados se pierden al reiniciar la aplicacion.
- En una etapa posterior, este store deberia reemplazarse por persistencia transaccional si se requiere auditoria o consulta historica.
