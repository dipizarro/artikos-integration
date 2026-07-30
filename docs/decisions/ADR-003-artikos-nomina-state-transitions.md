# ADR-003: Estados funcionales Artikos para confirmar y enviar resultados

## Estado

Aceptada

## Contexto

La aplicacion integra tres operaciones SOAP de Artikos QA:

- `NOMFACTERP`: consulta nominas disponibles.
- `NOMFACTCONFIR`: confirma recepcion de una nomina.
- `NOMFACTRES`: envia el resultado de procesamiento de la nomina.

Durante las pruebas se valido el mismo comportamiento desde la aplicacion y desde SoapUI. Esto confirma que los rechazos observados son reglas funcionales de Artikos QA y no errores de formato SOAP, token, `SOAPAction` o parsing local.

## Observacion

Que una nomina aparezca en `NOMFACTERP` no garantiza que pueda confirmarse o procesarse inmediatamente.

Artikos aplica validaciones de estado por operacion:

- `NOMFACTCONFIR` acepta la nomina solo cuando esta en estado `En Integracion`.
- `NOMFACTRES` acepta el resultado solo cuando la nomina esta en estado `Recibida`.

Errores funcionales observados:

```text
Solo se puede confirmar la recepcion de una nomina con estado En Integracion
```

```text
Solo se pueden procesar nominas en estado "Recibida"
```

## Decision

El batch tratara estos mensajes como rechazos funcionales de Artikos.

Para no perder trazabilidad, las actualizaciones de `CONTROL_NOMINA` se ejecutan en transacciones independientes. Asi, si el job termina `FAILED`, el registro funcional queda igualmente persistido con `STATUS=ERROR` y el mensaje de Artikos.

Cuando `NOMFACTCONFIR` responde que la nomina no esta en estado `En Integracion`, el batch permite continuar hacia el procesamiento local y el envio de `NOMFACTRES`. Si `NOMFACTRES` tambien rechaza por estado, esa respuesta final se registra en `CONTROL_NOMINA`.

## Consecuencias

- Una prueba integral real requiere una nomina en el estado funcional correcto.
- `NOMFACTERP` debe considerarse una consulta de candidatas, no una garantia de que la nomina pueda avanzar de estado.
- `dryRun=true` permite validar consulta y parsing sin modificar estado en Artikos.
- Para diagnostico funcional, se debe comparar el estado esperado de Artikos con la operacion ejecutada:
  - antes de `NOMFACTCONFIR`: `En Integracion`.
  - antes de `NOMFACTRES`: `Recibida`.
