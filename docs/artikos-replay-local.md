# Artikos captured XML local replay

## Objetivo

Definir el procedimiento para ejecutar localmente una nomina real Artikos extraida manualmente desde SoapUI, antes de ejecutar el flujo remoto completo Artikos + Adapter + Procurement.

Este replay no agrega endpoints, no consulta `NOMFACTERP`, no confirma `NOMFACTCONFIR` real y no envia `NOMFACTRES` real a Artikos. Usa el soporte existente `artikos.source.mode=local-xml`.

## Cuándo usarlo

Usar este procedimiento cuando:

- Artikos disponibiliza una nomina real en QA.
- El XML fue extraido manualmente desde SoapUI.
- Se quiere validar parser, lookup ASI, mapper Procurement, POST Procurement, `NOMFACTRES` local y `CONTROL_NOMINA` antes de correr remoto.

## Paso 1: ubicar el XML capturado

Ruta esperada para este sprint:

```properties
classpath:samples/ZSVIDA_Nom15965_v2.xml
```

Si el XML contiene datos reales sensibles y no debe versionarse, usar una ruta externa local:

```properties
file:C:/secure-local/artikos/ZSVIDA_Nom15965_v2.xml
```

La carpeta `src/test/resources/samples/artikos/captured/` queda disponible para XML capturados sanitizados o autorizados.

## Paso 2: configurar modo local XML

Configurar o revisar:

```properties
artikos.source.mode=local-xml
artikos.source.local-xml-path=classpath:samples/ZSVIDA_Nom15965_v2.xml
artikos.confirm.enabled=false
artikos.result.enabled=false

procurement.integration.enabled=true
procurement.client.enabled=true
procurement.client.base-url=<PROCUREMENT_QA_OR_LOCAL_URL>
```

Si se quiere revisar payloads de Procurement, habilitar la configuracion de debug que corresponda al entorno local:

```properties
procurement.debug.save-request=true
procurement.debug.save-response=true
procurement.debug.output-path=<LOCAL_DEBUG_PATH>
```

No versionar rutas locales privadas ni payloads con datos reales si no estan sanitizados.

## Paso 3: levantar la aplicacion local

Comando de arranque con perfil local y archivo de configuracion local:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--spring.config.additional-location=file:./src/main/resources/application-local.properties"
```

## Paso 4: ejecutar el batch

Endpoint:

```http
POST /api/v1/nominas/batch/start
```

Body:

```json
{
  "profile": "VIDA",
  "dryRun": false
}
```

Ejemplo PowerShell:

```powershell
$body = @{
  profile = "VIDA"
  dryRun = $false
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/nominas/batch/start" `
  -ContentType "application/json" `
  -Body $body
```

## Paso 5: validar resultado

Validar que:

- el parser leyo `numeroNomina`;
- lookup ASI resolvio items;
- JSON Procurement fue generado;
- `POST /api/v1/document` respondio `0`, `-20` o NOK funcional;
- `CONTROL_NOMINA` fue actualizado;
- `NOMFACTRES` fue generado localmente;
- no se llamo `NOMFACTCONFIR` real;
- no se envio `NOMFACTRES` real a Artikos.

Logs esperados:

```text
sourceMode=local-xml
Skipping Artikos NOMFACTCONFIR
operation=PROCUREMENT_POST_DOCUMENT
Skipping Artikos NOMFACTRES send
```

Consulta sugerida:

```sql
SELECT JOB_EXECUTION_ID,
       NUMERO_NOMINA,
       TOTAL_DOCUMENTS,
       TOTAL_OK,
       TOTAL_NOK,
       STATUS,
       ERROR_MESSAGE,
       CREATED_AT,
       UPDATED_AT
FROM CONTROL_NOMINA
ORDER BY CREATED_AT DESC;
```

## Checklist antes de ejecutar

- [ ] XML capturado guardado.
- [ ] XML sanitizado o autorizado si se va a versionar.
- [ ] `artikos.source.mode=local-xml`.
- [ ] `artikos.confirm.enabled=false`.
- [ ] `artikos.result.enabled=false`.
- [ ] `procurement.integration.enabled=true`.
- [ ] `procurement.client.enabled=true`.
- [ ] Conexion Oracle disponible.
- [ ] Periodo/configuracion ASI correcta.
- [ ] Logs en nivel adecuado.
- [ ] Debug request/response activado si se quiere revisar JSON.

## Checklist despues de ejecutar

- [ ] `CONTROL_NOMINA` con status `OK`, `NOK` o `ERROR`.
- [ ] `totalDocuments` correcto.
- [ ] `totalOk` y `totalNok` correctos.
- [ ] Request Procurement guardado si debug esta activo.
- [ ] Response Procurement guardado si debug esta activo.
- [ ] `NOMFACTRES` generado.
- [ ] `statusCode=-20` interpretado como OK idempotente si aplica.
- [ ] Errores funcionales distintos a duplicado interpretados como NOK.
- [ ] No hubo llamada real a `NOMFACTCONFIR`.
- [ ] No hubo envio real de `NOMFACTRES`.

## Criterios para pasar a prueba remota real

Solo pasar a integracion real remota si:

- el XML real capturado se procesa localmente sin error tecnico;
- Procurement responde OK o `-20` idempotente para los documentos esperados;
- no hay `ProcurementMappingException`;
- no hay lookup ASI faltante;
- `NOMFACTRES` generado tiene `CantidadInformados`, `CantidadOK` y `CantidadNOK` correctos;
- `CONTROL_NOMINA` queda `OK` o `NOK` funcional esperado, no `ERROR` tecnico;
- se valido que el XML corresponde a la misma nomina que se va a procesar remotamente;
- el equipo confirma que la nomina sigue disponible en Artikos para confirmacion/procesamiento real.

## Diferencia con local E2E

- `docs/local-e2e-testing.md`: pruebas locales con XML de ejemplo o fixture de desarrollo.
- `docs/artikos-replay-local.md`: replay local con XML real capturado desde SoapUI antes de una prueba remota real.

## Paso siguiente

Si el replay local funciona sin error tecnico y el equipo confirma que la nomina sigue disponible en Artikos, el siguiente paso es ejecutar la prueba remota real:

```text
docs/artikos-remote-e2e.md
```

## Seguridad de datos

- No subir tokens, passwords, URLs privadas ni XML con datos sensibles no autorizados.
- Si se requiere guardar evidencia, usar ejemplos sanitizados.
- Para XML reales no versionables, usar `file:` con una ruta local segura.
