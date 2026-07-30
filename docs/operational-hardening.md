# Operational Hardening

## Batch queue consumption

The real Artikos flow consumes nominas as a queue.

`ArtikosNominaItemReader` calls `NOMFACTERP` repeatedly:

- when Artikos returns a nomina, the reader emits one item;
- when Artikos returns `No hay nominas para procesar`, the reader returns `null` and the step finishes normally;
- when `maxNominas` is reached, the reader returns `null` and logs that the operational safety limit was reached.

`maxNominas` is not the business completion rule. It is a guardrail for controlled runs, QA tests and unexpected long loops.

## Processing unit

The batch item is a nomina, not a document.

Each nomina can contain a variable number of documents, conciliations and distributions. `ResultadoNomina` totals must always be calculated from the actual parsed XML.

## Failure policy

The job should finish `COMPLETED` when the queue is empty or when the safety limit is reached without errors.

The job should finish `FAILED` for:

- technical `NOMFACTERP` fetch failures;
- rejected `NOMFACTCONFIR`;
- rejected or failed `NOMFACTRES`;
- Oracle failures while writing `CONTROL_NOMINA`.

## Operational controls

Recommended production controls:

- set `atk.batch.default-max-nominas` and `atk.batch.max-nominas-per-run` to safe values for the execution window;
- keep `atk.batch.real.chunk-size=1` while Artikos state transitions are one nomina at a time;
- monitor logs for the `maxNominas` safety-limit message;
- review `CONTROL_NOMINA` after failed jobs before retrying.

## SOAP timeouts

Artikos SOAP calls use configurable HTTP timeouts:

```properties
artikos.http.connect-timeout-ms=5000
artikos.http.read-timeout-ms=30000
```

These values protect worker threads from hanging indefinitely on network or remote-service issues.

## Technical retries

Technical retries are controlled by:

```properties
artikos.retry.enabled=true
artikos.retry.max-attempts=3
artikos.retry.backoff-ms=1000
```

Retries are applied only to technical failures:

- timeouts and connection errors surfaced by `RestClient`;
- HTTP `5xx` responses from Artikos.

Retries are not applied to functional responses:

- valid SOAP responses with `MsgStatus != 0`;
- `No hay nominas para procesar`;
- business-state rejections from `NOMFACTCONFIR` or `NOMFACTRES`.

Each retry log includes operation, profile, attempt, max attempts and elapsed time. Tokens are never logged in full.

## Max nominas limit

`maxNominas` is a safety limit, not the business completion rule. The queue completion rule remains the Artikos response `No hay nominas para procesar`.

If the request omits `maxNominas`, the service uses:

```properties
atk.batch.default-max-nominas=50
```

Every request is capped by:

```properties
atk.batch.max-nominas-per-run=50
```

If `request.maxNominas` exceeds the configured cap, `POST /api/v1/nominas/batch/start` returns HTTP `400`.

## Profile concurrency

Only one active execution is allowed per profile:

- a running `VIDA` job blocks another `VIDA` job;
- a running `GENERALES` job blocks another `GENERALES` job;
- `VIDA` and `GENERALES` can run in parallel.

Active statuses are `STARTING`, `STARTED` and `STOPPING`. A profile conflict returns HTTP `409`.

## Health checks

`ArtikosConfigurationHealthIndicator` validates local configuration only. It does not call Artikos and does not change Artikos state.

The indicator checks:

- nomina endpoint configured;
- connector endpoint configured;
- profiles `VIDA` and `GENERALES` configured;
- consumo, respuesta and resultado operations configured;
- token presence without exposing token values.

Health details expose booleans such as `profilesConfigured`, `endpointsConfigured` and `diagnosticsEnabled`.

## Admin endpoint

The Spring Batch metadata purge endpoint is loaded only when:

```properties
app.admin.enabled=true
```

The default is `false`. Local and test environments may enable it, but production must protect administrative endpoints with corporate authentication and authorization before use.
