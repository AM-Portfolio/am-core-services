# SPT registration (load testing)

Each service declares a **minimal** `spt.yaml` next to `observability.yaml`.
SPT discovers APIs from the live OpenAPI document and owns auth.

## Java / Spring — expose OpenAPI (required)

Use **springdoc-openapi** (already on gateway / mcp / analysis).

Standard path (match SPT default for `runtime: java`):

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

Keep docs **auth-protected** (same JWT as APIs). SPT fetches with platform identity.

| Service | Live today | After next deploy |
|---------|------------|-------------------|
| am-analysis | `/v3/api-docs` | same |
| am-gateway | `/api-docs` (spt.yaml points here until redeploy) | `/v3/api-docs` |
| am-mcp-server | config → `/v3/api-docs` | same |

## File: `services/<name>/spt.yaml`

```yaml
apiVersion: am.spt/v1
kind: ServiceLoadTest
service: am-analysis
label: am-analysis
enabled: true
runtime: java                 # java | python
owners: [core-services]
createdBy: core-services
updatedBy: core-services
createdAt: "2026-07-22"
updatedAt: "2026-07-22"
source:
  repo: am-core-services
  path: services/am-analysis/spt.yaml
traces:
  - { name: configmap, ref: spt-catalog-am-analysis }
  - { name: onboarding, ref: docs/spt-onboarding.md }
targets:
  dev: "http://am-analysis.am-apps-dev.svc.cluster.local:8080"
  preprod: "http://am-analysis.am-apps-preprod.svc.cluster.local:8080"
  prod: "http://am-analysis.am-apps-prod.svc.cluster.local:8080"
openapi:
  path: /v3/api-docs          # java; python: /openapi.json
```

### Rules

- Pass **per-env base URLs** in `targets` — SPT picks `targets[environment]` automatically.
- Set `runtime: java` or `python`.
- Prefer `owners`, `createdBy`/`updatedBy`, `source.repo`/`path`, and `traces` for Specs **Traceability**.
- **Do not** put auth/tokens here — SPT uses platform identity login.
- **Do not** hand-maintain `apis: []` — SPT loads OpenAPI from `{target}{openapi.path}`.

## Publish to SPT

```bash
python scripts/publish-spt-catalogs.py
```

Creates ConfigMaps `spt-catalog-<service>` in namespace `load-testing`.

Validate:

```bash
python ../am-agents/poc/spt/scripts/validate-spt-yaml.py services
```
