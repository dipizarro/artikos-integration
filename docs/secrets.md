# Secrets and environment configuration

La aplicacion no debe versionar secretos reales. Los perfiles `qa` y `prod` usan placeholders que deben resolverse desde variables inyectadas por pipeline/runtime o desde Azure Key Vault.

## Strategy

- Local: usar `src/main/resources/application-local.properties`, ignorado por Git.
- Local example: usar `src/main/resources/application-local.example.properties` como plantilla segura.
- QA/PROD: usar `application-qa.properties` y `application-prod.properties` sin secretos reales.
- Azure: preferir Azure Key Vault con Managed Identity cuando la aplicacion corra en Azure.
- Runtime: el pipeline o plataforma debe inyectar variables compatibles con los placeholders Spring.
- Logging: no loguear passwords, tokens ni XML SOAP con token completo.
- Diagnostics: no exponer endpoints de configuracion con tokens completos; usar solo respuestas enmascaradas.

## Expected Secrets

| Logical name | Spring property | Environment | Description | Recommended Azure Key Vault name |
| --- | --- | --- | --- | --- |
| `APP_DATASOURCE_URL` | `app.datasource.url` | qa/prod | JDBC URL Oracle funcional ASI | `atk-nomina-batch-qa-app-db-url` |
| `APP_DATASOURCE_USERNAME` | `app.datasource.username` | qa/prod | Usuario Oracle funcional ASI | `atk-nomina-batch-qa-app-db-username` |
| `APP_DATASOURCE_PASSWORD` | `app.datasource.password` | qa/prod | Password usuario Oracle funcional ASI | `atk-nomina-batch-qa-app-db-password` |
| `BATCH_DATASOURCE_URL` | `batch.datasource.url` | qa/prod | JDBC URL Oracle metadata Spring Batch | `atk-nomina-batch-qa-batch-db-url` |
| `BATCH_DATASOURCE_USERNAME` | `batch.datasource.username` | qa/prod | Usuario Oracle metadata Spring Batch | `atk-nomina-batch-qa-batch-db-username` |
| `BATCH_DATASOURCE_PASSWORD` | `batch.datasource.password` | qa/prod | Password usuario Oracle metadata Spring Batch | `atk-nomina-batch-qa-batch-db-password` |
| `SPRING_BATCH_JDBC_TABLE_PREFIX` | `spring.batch.jdbc.table-prefix` | qa/prod | Prefijo tablas metadata Batch, por ejemplo `BACHPROCESS.BATCH_` | `atk-nomina-batch-qa-spring-batch-table-prefix` |
| `ARTIKOS_NOMINA_URL` | `artikos.qa.endpoints.nomina-url` | qa/prod | Endpoint SOAP extractor `NOMFACTERP` | `atk-nomina-batch-qa-artikos-nomina-url` |
| `ARTIKOS_CONNECTOR_URL` | `artikos.qa.endpoints.connector-url` | qa/prod | Endpoint SOAP connector `NOMFACTCONFIR` y `NOMFACTRES` | `atk-nomina-batch-qa-artikos-connector-url` |
| `ARTIKOS_GENERALES_CONSUMO_TOKEN` | `artikos.qa.profiles.GENERALES.consumo-nomina.token` | qa/prod | Token Artikos GENERALES para `NOMFACTERP` | `atk-nomina-batch-qa-artikos-generales-consumo-token` |
| `ARTIKOS_GENERALES_RESPUESTA_TOKEN` | `artikos.qa.profiles.GENERALES.respuesta-nomina.token` | qa/prod | Token Artikos GENERALES para `NOMFACTCONFIR` | `atk-nomina-batch-qa-artikos-generales-respuesta-token` |
| `ARTIKOS_GENERALES_RESULTADO_TOKEN` | `artikos.qa.profiles.GENERALES.resultado-nomina.token` | qa/prod | Token Artikos GENERALES para `NOMFACTRES` | `atk-nomina-batch-qa-artikos-generales-resultado-token` |
| `ARTIKOS_VIDA_CONSUMO_TOKEN` | `artikos.qa.profiles.VIDA.consumo-nomina.token` | qa/prod | Token Artikos VIDA para `NOMFACTERP` | `atk-nomina-batch-qa-artikos-vida-consumo-token` |
| `ARTIKOS_VIDA_RESPUESTA_TOKEN` | `artikos.qa.profiles.VIDA.respuesta-nomina.token` | qa/prod | Token Artikos VIDA para `NOMFACTCONFIR` | `atk-nomina-batch-qa-artikos-vida-respuesta-token` |
| `ARTIKOS_VIDA_RESULTADO_TOKEN` | `artikos.qa.profiles.VIDA.resultado-nomina.token` | qa/prod | Token Artikos VIDA para `NOMFACTRES` | `atk-nomina-batch-qa-artikos-vida-resultado-token` |
| `ARTIKOS_GENERALES_MSG_COD_FROM_ADDRESS` | `artikos.qa.profiles.GENERALES.*.msg-cod-from-address` | qa/prod | RUT/codigo emisor GENERALES | `atk-nomina-batch-qa-artikos-generales-msg-cod-from-address` |
| `ARTIKOS_GENERALES_MSG_COD_EXTERNO` | `artikos.qa.profiles.GENERALES.*.msg-cod-externo` | qa/prod | Codigo externo GENERALES | `atk-nomina-batch-qa-artikos-generales-msg-cod-externo` |
| `ARTIKOS_VIDA_MSG_COD_FROM_ADDRESS` | `artikos.qa.profiles.VIDA.*.msg-cod-from-address` | qa/prod | RUT/codigo emisor VIDA | `atk-nomina-batch-qa-artikos-vida-msg-cod-from-address` |
| `ARTIKOS_VIDA_MSG_COD_EXTERNO` | `artikos.qa.profiles.VIDA.*.msg-cod-externo` | qa/prod | Codigo externo VIDA | `atk-nomina-batch-qa-artikos-vida-msg-cod-externo` |

Para produccion, reemplazar el segmento `qa` del nombre sugerido por `prod`.

## Validation

La propiedad `app.config.validation.strict` controla la validacion de configuracion Artikos al arranque:

- `false`: no falla el arranque si faltan valores Artikos. Es el default para local/test.
- `true`: valida endpoints, perfiles `VIDA` y `GENERALES`, tokens, `msgCode`, `msgFromAddress` y `msgCodSis`.

QA y PROD deben usar:

```properties
app.config.validation.strict=true
```

Si falta un secreto o valor obligatorio, la aplicacion debe fallar temprano durante el arranque.
