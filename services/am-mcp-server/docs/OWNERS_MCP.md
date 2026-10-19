# Platform MCP — ownership (single runtime home)

**Runtime SoT:** this directory — `am-core-services/services/am-mcp-server`.

All new platform MCP **server** code lands here only:

- Domain enrich `@Tool`s (`MarketTools`, `PortfolioTools`, …)
- Contract pack loader (`com.am.mcp.contract.*`, `am.contract-mcp.*`)
- Auth, Helm, deploy (`.am.yaml`), SSE transport

GitHub repo for deploy: `AM-Portfolio/am-core-services` (see `.am.yaml`).  
Gitops values: `services/am-mcp-server/helm` in this repo.

## What belongs elsewhere

| Place | Owns | Must not own |
|-------|------|----------------|
| **am-specs** | OpenAPI → `tools.json` / `mcp/pack.json` generators | Java MCP server, Spring AI tools |
| **am-qa-agents** | SpecKit docs / E2E product face | A second MCP server process |
| **am-platform / am-mcp-hub** | Integration hub (Grafana, Vault, …) | Asrax OpenAPI contract/domain MCP |
| **am-fin-agent** `shared/mcp_server` | Fin-agent scoped meta-tools | Platform fleet Asrax API MCP |

## Contract packs

Generated in am-specs; **consumed** at runtime via `am.contract-mcp.packs-dir`.  
Details: [CONTRACT_MCP.md](CONTRACT_MCP.md).

## Cursor / IDE face (product router)

| Place | Owns |
|-------|------|
| **This service** | Fat runtime (domain `@Tool`s + all contract packs on DEV) |
| **amctl** `ai-catalog/mcp/bridges/am_mcp_server_stdio_proxy.py` | Thin Cursor face (search → describe → call) |
| **amctl** `ai-catalog/mcp/profiles/asrax-*.yaml` | Product vs fleet profile allowlists |

Do **not** register the full pack catalog as Cursor MCP tools.  
Do **not** add a second Java MCP process for Gemini — add an am-specs pack + product profile line.

## Rule for agents / PRs

If the change is “how agents call Asrax over MCP,” edit **this service**.  
If the change is “OpenAPI → pack JSON,” edit **am-specs**.  
If the change is “how Cursor discovers product tools,” edit **amctl** router/profiles.
