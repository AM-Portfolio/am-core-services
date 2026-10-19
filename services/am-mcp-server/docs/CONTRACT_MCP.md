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

- **Pack present** (`tools.json`) → register that service.
- Execution uses pack `_meta` (method/path/base_urls).
- **Allowlist empty** = every `tools.json` under packs-dir.
- **`AM_CONTRACT_MCP_ALL_GROUPS=true`** = all groups (admin/trading/…); otherwise only `core`.

## Sync packs (required before image build)

```bash
# from am-mcp-server
python scripts/sync_contract_packs.py
# copies am-specs/services/*/tools.json → contract-packs/services/
```

## Enable locally

```bash
export AM_CONTRACT_MCP_ENABLED=true
export AM_CONTRACT_MCP_PACKS_DIR=F:/am-repos/am-repos/am-specs/services
export AM_CONTRACT_MCP_ENV=dev
export AM_CONTRACT_MCP_ALL_GROUPS=true
mvn spring-boot:run
```

Status: `GET /api/v1/contract-mcp/status`  
Reload: `POST /api/v1/contract-mcp/reload`

## Cluster (DEV)

Image bakes **all** am-specs `tools.json` packs at `/packs/services`. Env:

- `AM_CONTRACT_MCP_ENABLED=true`
- `AM_CONTRACT_MCP_PACKS_DIR=/packs/services`
- `AM_CONTRACT_MCP_ENV=dev`
- `AM_CONTRACT_MCP_ALL_GROUPS=true`

Deploy:

```bash
python scripts/sync_contract_packs.py
am deploy --env dev --via gitops --rebuild
```

SSE prove (DEV): `python scripts/mcp_sse_pilot_prove.py`

## QA Specs catalog

After MCP packs are SoT, refresh QA Spec UI registrations from the same tools.json:

```bash
# am-qa-agents
python qa-agent/scripts/sync_catalog_from_am_specs.py --apply-dev
```

## Product vs fleet consumers

| Consumer | Tool face | Catalog |
|----------|-----------|---------|
| **SpecKit / SSE prove / CI** | Full MCP SSE (`/mcp/sse`) | All baked packs (fleet) |
| **Cursor IDE (`asrax`)** | Product **router** (~4 meta-tools) | Filtered by `AM_MCP_PROFILE=product` |

Cursor never receives hundreds of JSON schemas. The stdio proxy
(`amctl` → `~/.asrax/bin/am_mcp_server_stdio_proxy.py`) advertises only:

`list_domains` → `search_tools` → `describe_tool` → `call_tool`

Profile SoT: `amctl/ai-catalog/mcp/profiles/asrax-product.yaml`  
(synced to `~/.asrax/mcp/profiles/` by `am ai sync`).

### Add one module later (e.g. Gemini)

1. Publish / enable `tools.json` pack in **am-specs** for the service.
2. Sync packs into this image (`python scripts/sync_contract_packs.py`) and redeploy DEV MCP.
3. Append the service id to `asrax-product.yaml` `services:` list.
4. Run `am ai sync` (refresh `~/.asrax/mcp/profiles`). Restart Cursor MCP `asrax`.

No new Cursor `mcp.json` server entry is required.

Fleet prove keeps using the fat server; do not shrink the DEV pack bake for IDE comfort.
