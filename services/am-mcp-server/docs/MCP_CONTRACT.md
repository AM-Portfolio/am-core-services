# AM MCP Server Contract v1.0.0

Shared data-tool API for fin-agent, Postman, Cursor, and other services.

## Version

`1.0.0` — bump when tool names, required args, or response envelope change in a breaking way.

## Transport

| Item | Value |
|------|--------|
| Base (in-cluster Dev) | `http://am-mcp-server.am-apps-dev.svc.cluster.local:8080` |
| Public Dev (via gateway) | `https://am-dev.asrax.in/mcp` (path strip may apply) |
| SSE open | `GET {base}/sse` with `Authorization: Bearer <user JWT>` |
| Messages | `POST {base}/message?sessionId=<id from SSE>` |
| Auth | Bearer user JWT required on SSE and message (not actuator) |

Discover tools with MCP `tools/list`. Call with `tools/call`.

## Response envelope (v1)

Every tool returns a JSON **string**:

**Success**

```json
{ "ok": true, "data": { } }
```

**Failure**

```json
{
  "ok": false,
  "error": "CODE",
  "message": "...",
  "retry": true,
  "tool": "tool_name",
  "hint": "optional"
}
```

Oversized payloads return `RESPONSE_TOO_LARGE` (valid JSON), never truncated mid-string.

## Identity

- Prefer the inbound JWT (`UserContext`) for outbound portfolio/market/trade calls.
- `userId` tool args are **optional** where present; server resolves JWT `sub` then config default.
- Do not rely on email as `userId` unless backends expect it.

## Tool catalog rules

- Source of truth: live `tools/list` from this service.
- Optional parameters use `@ToolParam(required = false)` and must not appear in JSON Schema `required[]`.
- `calculate_basket_quantities` takes `investmentAmount`, `etfIsin`, `portfolioId` (optional `userId`); server rebuilds opportunity — no JSON blob.
- `ask_finance_agent` is for human/Cursor multi-step escape; **fin-agent must blocklist** it to avoid recursion.

## Client checklist

1. Send user Bearer on every MCP session.
2. Parse envelope: use `data` when `ok=true`; surface `error`/`message` when `ok=false`.
3. Pin expectations with `npm run list-tools:json` after deploys.
4. Treat tool renames as breaking; prefer add + deprecate.

## Related

- Fin-agent freeze: `am-agents/catalog/finance/MCP_FREEZE.md`
- List script: `services/am-mcp-server/scripts/list_tools.py`
