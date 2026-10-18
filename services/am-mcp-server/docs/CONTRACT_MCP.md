# Contract MCP (am-specs packs)

## Home repo

**Platform MCP runtime lives only here:** `am-core-services/services/am-mcp-server`.  
Ownership boundaries (am-specs packs vs hub vs fin-agent): [OWNERS_MCP.md](OWNERS_MCP.md).

---

Agents talk to **am-mcp-server** (MCP HTTP/SSE). Curated domain `@Tool`s stay as today.
When **contract MCP** is enabled, the server also loads OpenAPI tools from am-specs packs.

## Model

```text
Agent → am-mcp-server
          ├─ Domain tools (MarketTools, PortfolioTools, …)  — curated Java client libs
          └─ Contract tools (api_<service>_<opId>)         — if packs present
                → HTTP using tools.json _meta + executor auth
```

- **Pack present** (`tools.json`, ideally with `mcp/pack.json`) → register that service.
- Execution uses pack `_meta` (method/path/base_urls). Same OpenAPI surface as published
  `com.asrax:*` Java SDKs; HTTP avoids a Maven dependency per fleet service.
- Default **group filter** = `core` (from `grouping.yaml`) so agents are not flooded.

## Enable locally

```bash
# from am-core-services/services/am-mcp-server
export AM_CONTRACT_MCP_ENABLED=true
export AM_CONTRACT_MCP_PACKS_DIR=F:/am-repos/am-repos/am-specs/services
export AM_CONTRACT_MCP_ENV=dev
# optional: AM_CONTRACT_MCP_ALL_GROUPS=true
mvn spring-boot:run
```

Status: `GET /api/v1/contract-mcp/status`  
Reload packs without restart: `POST /api/v1/contract-mcp/reload`

## Cluster

1. Publish/sync am-specs service packs (or mount `services/` from a volume).
2. Set env:
   - `AM_CONTRACT_MCP_ENABLED=true`
   - `AM_CONTRACT_MCP_PACKS_DIR=/packs/services`
   - `AM_CONTRACT_MCP_ENV=dev|preprod|prod`
3. Mount packs at that path (ConfigMap is too small for full OpenAPI fleets — use PVC or init-container git sync).

## Allowlist

Default services: `am-market-data`, `am-news`, `am-parser` (SpecKit pilots).
Override `am.contract-mcp.services` to expand the fleet once packs exist.
