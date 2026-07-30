# ADR-002: Package namespace

## Status

Accepted

## Context

El package base original del proyecto era:

```text
cl.poc.atkbatch
```

El servicio ya evoluciono desde una prueba inicial hacia una aplicacion batch real para integracion Artikos. El nombre del artefacto y la documentacion principal ya fueron normalizados.

En Sprint 8.6.1 se confirmo el namespace corporativo objetivo:

```text
cl.atk.nomina.batch
```

## Decision

Migrar el namespace base Java desde `cl.poc.atkbatch` hacia `cl.atk.nomina.batch`.

## Consequences

- El namespace queda alineado con el estandar validado para la aplicacion.
- Imports, packages, tests y configuracion de logs deben mantenerse bajo `cl.atk.nomina.batch`.
- Se elimina la deuda tecnica asociada al namespace historico con referencia a POC.

## Next Step

Mantener nuevas clases bajo `cl.atk.nomina.batch` y evitar reintroducir paquetes `cl.poc.atkbatch`.
