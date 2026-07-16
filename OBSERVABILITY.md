# Observability (Plane A)

Each deployable service has `services/<name>/observability.yaml`.

- **Bundle:** `tier-a-java-core` → shared Grafana **Technical / Services**
- **Scrape:** each service `helm/values.yaml` `podAnnotations` (`prometheus.io/scrape`)
- **Metrics:** `micrometer-registry-prometheus` (via service pom + `am-observability-lib`) → `/actuator/prometheus`
- **Dropdown:** Grafana discovers `application=` from Prometheus (`label_values`); no am-obs registry entry
- **Validate CI:** `.github/workflows/observability-validate.yml` runs doctor from **[AM-Portfolio/am-observability](https://github.com/AM-Portfolio/am-observability)** (`vars.AM_OBS_REF` or `main`)

## Service → Grafana Service label

| Service | `spring.application.name` / `metrics_application` |
|---------|-----------------------------------------------------|
| am-gateway | `am-websocket-gateway` |
| am-analysis | `am-analysis` |
| am-mcp-server | `am-mcp-server` |

After deploy + scrape, open **AM / Technical → Technical / Services**, pick that label.

## Optional Plane C (hide panels)

Set `dashboard.technical.rows` in the service yaml, then:

```bash
cd ../am-observability   # or clone AM-Portfolio/am-observability
python gen.py compose-view --manifest ../am-core-services/services/am-gateway/observability.yaml
kubectl apply -f dist/grafana/tech-view-am-gateway.yaml
```
