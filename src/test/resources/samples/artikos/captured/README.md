# Captured Artikos XML samples

This folder is reserved for Artikos XML responses captured manually with tools such as SoapUI.

Only commit captured XML files when they are sanitized or explicitly authorized for repository use.

Do not commit files that contain:

- real secrets or tokens;
- private endpoint URLs;
- sensitive supplier or person data;
- business data that has not been approved for version control.

For local testing with non-versioned files, prefer an external path:

```properties
artikos.source.mode=local-xml
artikos.source.local-xml-path=file:C:/secure-local/artikos/ZSVIDA_Nom15965_v2.xml
```

The replay procedure is documented in `docs/artikos-replay-local.md`.
