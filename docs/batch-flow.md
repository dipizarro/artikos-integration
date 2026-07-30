# Flujo Batch

## Inicio

El flujo inicia con:

```http
POST /api/v1/nominas/batch/start
```

Request tipico:

```json
{
  "profile": "VIDA",
  "maxNominas": 1000,
  "dryRun": false
}
```

La respuesta es inmediata e incluye `jobExecutionId`, `jobName`, `status`, `profile`, `maxNominas` y `dryRun`.

`maxNominas` es un limite operativo de seguridad. No representa la cantidad esperada de nominas ni la condicion principal de termino del proceso.

## Job

- Job: `nominaDocumentosContablesJob`
- Step real: `processNominaDocumentosStep`
- Chunk real recomendado: `atk.batch.real.chunk-size=1`

## Flujo operacional

1. `ArtikosNominaItemReader` obtiene nominas desde la fuente configurada en `artikos.source.mode`.
2. En modo `remote`, consulta Artikos con `NOMFACTERP`.
3. En modo `local-xml`, lee una nomina desde `artikos.source.local-xml-path` y no consume Artikos real.
4. Si Artikos retorna una nomina, el reader la entrega como un item del step.
5. Si Artikos responde `No hay nominas para procesar`, el reader retorna `null` y el step termina normalmente.
6. Si se alcanza `maxNominas`, el reader retorna `null`, registra el limite alcanzado y el step termina normalmente.
7. `ArtikosNominaItemProcessor` registra `CONTROL_NOMINA` en `PROCESSING`.
8. Si `dryRun=false` y el modo es `remote`, se confirma recepcion con `NOMFACTCONFIR`.
9. En modo `local-xml`, se omite `NOMFACTCONFIR` aunque `artikos.confirm.enabled=true`.
10. `NominaProcessingService` procesa todos los documentos reales de la nomina. Por defecto usa validaciones locales; si `procurement.integration.enabled=true`, envia cada documento a Procurement `POST /api/v1/document`.
11. `NominaResultXmlService` genera el XML `NOMFACTRES`.
12. `ArtikosNominaResultItemWriter` envia `NOMFACTRES` si `dryRun=false`, `artikos.result.enabled=true` y el modo es `remote`.
13. En modo `local-xml`, se genera `NOMFACTRES` pero no se envia a Artikos.
14. `CONTROL_NOMINA` se actualiza con `OK`, `NOK` o `ERROR`.
15. `BatchResultStore` mantiene resultados en memoria para consultas operacionales del job.

## Termino del reader

La condicion principal de termino es la respuesta funcional de Artikos indicando que no hay mas nominas disponibles. El limite `maxNominas` solo protege contra ejecuciones demasiado largas o loops inesperados.

La propiedad `atk.batch.default-max-nominas` define el valor por defecto cuando el request no lo informa. La propiedad `atk.batch.max-nominas-per-run` define el maximo permitido por ejecucion. Puede sobreescribirse por request para pruebas o ventanas operativas acotadas, siempre dentro del limite configurado.

## Unidad de procesamiento

La unidad principal del batch real es la nomina. Una nomina puede contener una cantidad variable de documentos, conciliaciones y distribuciones.

Los totales de `ResultadoNomina` se calculan dinamicamente desde el XML recibido:

- `totalDocuments`: cantidad real de documentos.
- `totalOk`: documentos procesados sin observaciones locales o aceptados por Procurement.
- `totalNok`: documentos rechazados por reglas funcionales locales o por `statusCode!=0` de Procurement.
- `totalConciliaciones`: suma real de conciliaciones.
- `totalDistribuciones`: suma real de distribuciones.

## Procesamiento documental

El procesamiento documental se resuelve por `DocumentProcessingService`:

- `procurement.integration.enabled=false`: se usa `SimulatedDocumentProcessingService`, que conserva las validaciones locales existentes.
- `procurement.integration.enabled=true`: se usa `ProcurementDocumentProcessingService`, que llama Procurement una vez por documento.

La respuesta funcional de Procurement se interpreta asi:

- `statusCode=0`: documento `OK`.
- `statusCode=-20`: documento `OK` idempotente.
- `statusCode!=0` distinto a duplicado: documento `NOK`, sin fallar el job.

Los errores tecnicos de Procurement o errores de mapeo Artikos -> CMP fallan la nomina, marcan `CONTROL_NOMINA` como `ERROR` y evitan el envio de `NOMFACTRES`.

## Dry run

Con `dryRun=true`, el servicio consulta Artikos y procesa siempre con la ruta simulada/local, aunque `procurement.integration.enabled=true`. No confirma recepcion, no envia resultado y no llama Procurement. Este modo sirve para validar parsing, reglas internas y resumen batch sin alterar estado en Artikos ni en sistemas externos.

## Modo local XML

`artikos.source.mode=local-xml` permite validar el pipeline tecnico usando un XML local como fuente cuando QA Artikos no tiene nominas disponibles. Este modo no consulta `NOMFACTERP`, no confirma `NOMFACTCONFIR` y no envia `NOMFACTRES` real. Si Procurement esta habilitado, si puede llamar Procurement.

Detalle operativo en `docs/local-e2e-testing.md`.

## Errores funcionales

Los rechazos SOAP con `MsgStatus=1` se tratan como errores funcionales. El detalle devuelto por Artikos se conserva en la respuesta del endpoint o en `CONTROL_NOMINA.ERROR_MESSAGE`, segun el punto del flujo.

## Estados Artikos relevantes

- `NOMFACTCONFIR` requiere nomina en estado `En Integracion`.
- `NOMFACTRES` requiere nomina en estado `Recibida`.

Una nomina puede seguir apareciendo en `NOMFACTERP` aunque no este lista para confirmacion o resultado.
