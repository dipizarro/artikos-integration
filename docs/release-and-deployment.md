# Release and deployment

## 1. Objetivo

Este documento define el procedimiento operativo para llevar cambios de `artikos-integration` desde el repositorio corporativo hasta la infraestructura del cliente, manteniendo trazabilidad entre ambas plataformas.

El modelo utiliza dos repositorios con responsabilidades diferentes:

- **GitHub corporativo**: repositorio canónico y fuente de verdad del producto.
- **GitLab cliente**: repositorio utilizado para entrega, pipeline y despliegue en PRE/PROD del cliente.

La continuidad operativa, Issues, milestones, documentación, decisiones, Pull Requests y tags corporativos se mantienen en GitHub. GitLab recibe una réplica controlada del cambio aprobado que debe ser desplegado.

## 2. Principio de gobierno

```text
GitHub corporativo
SOURCE OF TRUTH

Issues
Milestones
Docs / ADR
Branches
Pull Requests
Tests
Tags / Releases
Continuidad operativa
        |
        | paquete de transferencia
        | git format-patch
        | + manifiesto
        | vía SharePoint
        v
GitLab cliente
DEPLOYMENT REPOSITORY

Branch
Merge Request
Pipeline
PRE
PROD
Tag
        |
        | resultado del despliegue
        v
GitHub corporativo

PR preproduccion -> produccion
Tag equivalente
Cierre / actualización de Issue
```

SharePoint funciona únicamente como canal autorizado de transferencia de archivos. No debe utilizarse para reconstruir manualmente fuentes mediante copy/paste.

## 3. Modelo objetivo de ramas corporativas

```text
produccion
    ^
    |
preproduccion
    ^
    |
feature/*
fix/*
hotfix/*
docs/*
```

Reglas:

- los cambios se desarrollan en ramas específicas;
- no se modifica directamente una rama protegida;
- todo cambio debe pasar por Pull Request;
- `preproduccion` representa cambios aprobados corporativamente y candidatos a promoción al cliente;
- `produccion` representa únicamente cambios que ya fueron desplegados y validados en producción del cliente.

### Bootstrap inicial de ramas

Al momento de crear este documento, `preproduccion` y `produccion` todavía no forman parte del repositorio corporativo.

No deben crearse simplemente desde `main` sin antes reconciliar el estado productivo real.

Procedimiento previo:

```text
GitLab PROD
   |
   v
identificar versión/tag actual
   |
   v
identificar contenido desplegado
   |
   v
buscar equivalente en GitHub
   |
   v
explicar cualquier drift
   |
   v
crear baseline corporativo
```

Solo después de esa reconciliación se deben inicializar `produccion` y `preproduccion` desde los commits acordados.

Hasta completar ese bootstrap, `main` continúa siendo la rama base vigente del repositorio corporativo.

## 4. Flujo completo de una incidencia o cambio

```text
Incidente / requerimiento
        |
        v
GitHub Issue
        |
        v
Branch de trabajo
        |
        v
Implementación
        |
        v
Tests / validación local
        |
        v
PR corporativo
        |
        v
Review / aprobación
        |
        v
Merge corporativo
        |
        v
git format-patch
        |
        v
SharePoint
        |
        v
git am en GitLab cliente
        |
        v
MR -> preproduccion cliente
        |
        v
Pipeline / deploy PRE
        |
        v
Validación funcional
        |
        v
Promoción PROD
        |
        v
Tag GitLab
        |
        v
Sincronización GitHub produccion
        |
        v
Tag GitHub equivalente
```

## 5. Resolver el cambio en GitHub corporativo

Crear una rama desde la rama corporativa correspondiente. Cuando el modelo `preproduccion`/`produccion` ya esté inicializado, los cambios normales deben nacer desde `preproduccion`.

Ejemplo:

```bash
git checkout preproduccion
git pull
git checkout -b fix/fec-comprb-fecha-nomina
```

Realizar commits lógicos y autocontenidos:

```bash
git add ...
git commit -m "fix: mapear FEC_COMPRB desde fecha nomina"

git add ...
git commit -m "test: cubrir mapping de FEC_COMPRB"

git add ...
git commit -m "docs: actualizar mapping procurement"
```

El Pull Request debe estar asociado al Issue y contener problema, solución, archivos afectados, pruebas, riesgos, evidencia e impacto esperado.

## 6. Registrar el rango exacto del PR

Antes de eliminar la rama del cambio, registrar como evidencia:

- Issue GitHub;
- PR GitHub;
- branch origen;
- commit base;
- commit final;
- lista de commits incluidos.

Para obtener el punto de divergencia:

```bash
git merge-base preproduccion fix/fec-comprb-fecha-nomina
```

Para revisar los commits exclusivos de la rama:

```bash
git log --oneline preproduccion..fix/fec-comprb-fecha-nomina
```

Ejemplo conceptual:

```text
BASE_SHA=abc123
HEAD_SHA=def456
```

No se debe deducir el rango después de que la branch haya sido eliminada si se puede registrar previamente.

## 7. Generar el paquete de transferencia con git format-patch

Generar exclusivamente los commits del cambio aprobado:

```bash
git format-patch BASE_SHA..HEAD_SHA
```

Resultado esperado:

```text
0001-fix-mapear-FEC_COMPRB-desde-fecha-nomina.patch
0002-test-cubrir-mapping-de-FEC_COMPRB.patch
0003-docs-actualizar-mapping-procurement.patch
```

No generar patches usando un rango amplio de `preproduccion` si ese rango puede contener commits pertenecientes a otros Pull Requests.

Validar antes de transferir:

```bash
git log --oneline BASE_SHA..HEAD_SHA
```

La cantidad y orden de commits deben corresponder al paquete generado.

Opcionalmente revisar el contenido de un patch:

```bash
git apply --stat 0001-*.patch
```

## 8. Manifest de transferencia

Junto al paquete debe mantenerse un manifiesto de trazabilidad con, como mínimo:

```text
Proyecto
Issue GitHub
PR GitHub
Branch origen
Base SHA
Head SHA
Commits transferidos
Branch GitLab destino
MR GitLab
Versión candidata
Estado PRE
Estado PROD
Tag final
```

Ejemplo:

```text
Proyecto: artikos-integration
GitHub Issue: #6
GitHub PR: #XX
Branch origen: fix/fec-comprb-fecha-nomina
Base SHA: abc123
Head SHA: def456
Target GitLab: preproduccion
Versión candidata: v1.2.1
Estado PRE: PENDIENTE
Estado PROD: PENDIENTE
```

No incluir secretos, tokens ni payloads sensibles.

El manifiesto puede mantenerse como evidencia del Issue/PR o en el mecanismo corporativo definido para el release.

## 9. Transferencia mediante SharePoint

El paquete esperado es:

```text
release/
    release-manifest.md
    0001-....patch
    0002-....patch
    0003-....patch
```

SharePoint se utiliza únicamente como puente de transferencia entre las plataformas corporativa y cliente.

El procedimiento anterior de copiar y pegar archivos fuente queda reemplazado por `git format-patch` + `git am`.

## 10. Aplicar el cambio en GitLab cliente

En el repositorio del cliente:

```bash
git checkout preproduccion
git pull
git checkout -b fix/fec-comprb-fecha-nomina
```

Copiar los archivos `.patch` desde el canal de transferencia a una ubicación local no versionada y aplicar:

```bash
git am *.patch
```

`git am` permite preservar la estructura lógica del cambio: contenido, autoría, mensaje, archivos agregados/modificados/eliminados y separación por commit.

## 11. SHA GitHub y SHA GitLab

No utilizar igualdad de SHA como único criterio de equivalencia.

Al aplicar los patches pueden generarse commits con SHA distintos en GitLab. Mantener una correspondencia cuando sea necesario:

```text
GitHub SHA        GitLab SHA
123aaa       ->   91ab01
456bbb       ->   33fc92
789ccc       ->   84da72
```

Para una comprobación técnica adicional puede utilizarse `patch-id`:

```bash
git show <sha> | git patch-id --stable
```

La trazabilidad debe demostrar equivalencia del cambio, aunque los identificadores de commit no sean idénticos.

## 12. Qué hacer si git am encuentra conflictos

Si `git am` falla:

```bash
git status
```

Antes de resolver manualmente, investigar el drift entre repositorios.

Posibles causas:

- cambios realizados directamente en GitLab;
- baseline diferente;
- commit ya aplicado;
- cambios históricos no sincronizados;
- contenido del cliente que GitHub no posee.

Para cancelar el intento:

```bash
git am --abort
```

No convertir la resolución manual del conflicto en procedimiento normal. Si se decide resolver un conflicto, la causa y la resolución deben quedar comprendidas y documentadas.

## 13. Validar la réplica en GitLab

Revisar:

```bash
git log --oneline
```

Comparar el cambio de la branch contra `preproduccion`:

```bash
git diff preproduccion...HEAD
```

El resultado funcional debe corresponder al Pull Request corporativo.

## 14. Crear Merge Request cliente

Crear el MR desde la branch equivalente hacia `preproduccion`.

El summary puede reutilizar y adaptar el Pull Request corporativo, agregando trazabilidad mínima:

```text
GitHub Issue: #<id>
GitHub PR: #<id>
Branch origen: <branch>
Head SHA origen: <sha>
Versión candidata: vX.Y.Z
```

El MR no reemplaza al Issue ni a la documentación corporativa. Su objetivo es revisión y despliegue dentro de la infraestructura cliente.

## 15. Pipeline y despliegue cliente

La `.gitlab-ci.yml` versionada declara las etapas:

```text
test
build
deploy
cleanup
```

Y utiliza componentes corporativos para:

- Docker lint;
- Docker build;
- Docker deploy;
- deploy review;
- SonarQube;
- Azure App Configuration;
- Azure Key Vault.

El pipeline versionado también referencia actualmente:

- namespace Kubernetes: `artikos`;
- recurso Flux: `artikos-integration`;
- repositorio IaC: `zs/zs-kubernetes/artikos/iac-artikos-integration.git`.

La implementación interna de esos componentes corresponde a la plataforma CI/CD del cliente y no se replica en esta documentación.

## 16. Validación PRE

Después del merge del MR a `preproduccion`, comprobar como mínimo:

1. pipeline finalizado correctamente;
2. deployment ejecutado;
3. aplicación/pod saludable;
4. `GET /actuator/health` responde correctamente;
5. logs corresponden a la versión esperada;
6. configuración del ambiente es la esperada;
7. dependencias externas necesarias están disponibles;
8. prueba funcional controlada del cambio es satisfactoria.

Principio crítico:

```text
Código mergeado != código desplegado
```

La presencia del cambio en Git no demuestra que el ambiente esté ejecutando esa versión.

## 17. Cambio mergeado pero comportamiento antiguo

Si el repositorio contiene la corrección pero PRE sigue mostrando el comportamiento anterior, investigar la cadena:

```text
commit esperado
      |
      v
imagen construida
      |
      v
deployment ejecutado
      |
      v
pod actual
      |
      v
versión / logs
```

No volver directamente a modificar código hasta confirmar qué versión está ejecutándose realmente.

## 18. Promoción a producción cliente

Una vez aprobada la validación PRE, promover el cambio siguiendo el procedimiento vigente del cliente.

Conceptualmente:

```text
preproduccion
      |
      v
produccion
      |
      v
pipeline
      |
      v
PROD
```

Volver a validar pipeline, health, versión/logs y comportamiento funcional.

No documentar comandos específicos de promoción o rollback que no estén confirmados por la plataforma cliente.

## 19. Tags equivalentes

Una vez confirmado el despliegue productivo, crear o actualizar el tag en GitLab cliente.

Ejemplo:

```text
v1.2.1
```

La misma versión debe utilizarse posteriormente en GitHub corporativo:

```text
GitLab cliente       GitHub corporativo
v1.2.1         <->       v1.2.1
```

El tag funciona como identificador transversal entre Issue, PR, commits, MR y despliegue productivo.

## 20. Sincronizar producción en GitHub

Solo después de confirmar producción cliente:

```text
GitHub preproduccion
      |
      v
PR -> produccion
      |
      v
Review
      |
      v
Merge
      |
      v
Tag equivalente
```

De esta forma, la rama `produccion` corporativa representa la realidad productiva del cliente.

## 21. Cierre y evidencia

Actualizar el seguimiento corporativo con, cuando corresponda:

```text
GitHub Issue
GitHub PR PRE
Base/Head SHA origen
GitLab MR
Resultado PRE
Resultado PROD
GitLab tag
GitHub PR PROD
GitHub tag
```

Una incidencia no se considera completamente desplegada únicamente porque el PR corporativo haya sido mergeado.

## 22. Rollback

Antes de decidir rollback revisar:

- versión previa conocida;
- impacto funcional del cambio;
- compatibilidad con configuración y base de datos;
- estado de nóminas parcialmente procesadas;
- estado Artikos/Procurement;
- evidencia del incidente.

No eliminar `CONTROL_NOMINA` ni metadata `BATCH_*` como mecanismo de rollback.

Los comandos específicos de rollback Kubernetes/Flux deben provenir del procedimiento corporativo de infraestructura del cliente.

## 23. Responsabilidades

| Área | Responsabilidad |
| --- | --- |
| GitHub corporativo | Source of truth, Issues, PR, documentación y tags corporativos |
| Desarrollo/mantenedor | Implementación, tests, patches y trazabilidad |
| SharePoint | Canal autorizado de transferencia |
| GitLab cliente | MR, pipeline, PRE/PROD y tag cliente |
| CI/CD | Build, calidad, imagen y deploy |
| IaC / Flux / Kubernetes | Estado desplegado y runtime |
| Operación / cliente | Validación funcional y aprobación de promoción |

## 24. Documentos relacionados

- `docs/onboarding.md`
- `docs/delivery-checklist.md`
- `docs/infra-delivery.md`
- `docs/environments-and-dependencies.md`
- `docs/runbook.md`
- `docs/support-guide.md`

## 25. Regla central

```text
GitHub decide qué es el producto.

GitLab decide qué llega a la infraestructura cliente.

PROD cliente confirma qué versión está realmente productiva.

GitHub produccion registra finalmente esa realidad.
```
