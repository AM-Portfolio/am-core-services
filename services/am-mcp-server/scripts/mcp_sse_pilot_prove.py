#!/usr/bin/env python3
"""Prove SpecKit pilot tools via am-mcp-server SSE on DEV only.

Pilots: am-market-data / am-news / am-parser.
Lists tools, calls domain + contract (api_*) tools, writes JSON + HTML.

Auth (first match):
  AM_MCP_BEARER_TOKEN
  dist/dev-mcp.token (from scripts/_tmp_dev_auth.py)
  AM_MCP_CLIENT_ID + AM_MCP_CLIENT_SECRET → KEYCLOAK_TOKEN_URL
  Vault apps/data/dev/services/am-identity admin → am-mcp-service secret on am-dev-realm
  ~/.asrax/credentials.d/asrax.dev.env client + am-dev-realm token URL

Env:
  AM_MCP_BASE   default https://am-dev.asrax.in/mcp
"""
from __future__ import annotations

import argparse
import html
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PILOTS = ("am-market-data", "am-news", "am-parser")
UA = "am-mcp-sse-pilot-prove/0.2"

DOMAIN_CASES: list[tuple[str, str, dict]] = [
    ("am-market-data", "get_stock_quote", {"symbol": "RELIANCE"}),
    ("am-market-data", "search_instruments", {"query": "RELIANCE"}),
    ("am-market-data", "get_historical_data", {"symbol": "RELIANCE", "interval": "DAY"}),
]


def _env(name: str, default: str = "") -> str:
    return (os.environ.get(name) or default).strip()


def _load_dotenv(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    if not path.exists():
        return out
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v.strip().strip('"').strip("'")
    return out


def _asrax_env() -> dict[str, str]:
    env: dict[str, str] = {}
    home = Path.home() / ".asrax"
    env.update(_load_dotenv(home / "credentials.env"))
    for name in ("asrax.dev.env", "asrax.env", "dev.env"):
        env.update(_load_dotenv(home / "credentials.d" / name))
    return env


def _http_json(url: str, *, data: bytes | None = None, headers: dict | None = None) -> dict:
    h = {"User-Agent": UA, "Accept": "application/json"}
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, data=data, headers=h, method="POST" if data else "GET")
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


def _client_credentials(token_url: str, client_id: str, client_secret: str) -> str:
    body = urllib.parse.urlencode(
        {
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
        }
    ).encode()
    data = _http_json(
        token_url,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    token = data.get("access_token") or ""
    if not token:
        raise RuntimeError("client_credentials missing access_token")
    return token


def _token_via_keycloak_admin(vault_addr: str, vault_token: str) -> str:
    """Fetch am-mcp-service secret from am-dev-realm via Keycloak admin."""
    id_path = f"{vault_addr.rstrip('/')}/v1/apps/data/dev/services/am-identity"
    req = urllib.request.Request(
        id_path,
        headers={"X-Vault-Token": vault_token, "User-Agent": UA},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        payload = json.loads(resp.read().decode())["data"]
        if "data" in payload:
            payload = payload["data"]
    admin = payload["KEYCLOAK_ADMIN_USER"]
    apw = payload["KEYCLOAK_ADMIN_PASSWORD"]
    kc = (payload.get("KEYCLOAK_URL") or "https://auth.asrax.in").rstrip("/")
    realm = payload.get("KEYCLOAK_REALM") or "am-dev-realm"

    body = urllib.parse.urlencode(
        {
            "grant_type": "password",
            "client_id": "admin-cli",
            "username": admin,
            "password": apw,
        }
    ).encode()
    admin_tok = _http_json(
        f"{kc}/realms/master/protocol/openid-connect/token",
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )["access_token"]

    client_id = "am-mcp-service"
    req = urllib.request.Request(
        f"{kc}/admin/realms/{realm}/clients?clientId={client_id}",
        headers={"Authorization": f"Bearer {admin_tok}", "User-Agent": UA},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        clients = json.loads(resp.read().decode())
    if not clients:
        raise RuntimeError(f"Keycloak client {client_id} missing in {realm}")
    cid = clients[0]["id"]
    req = urllib.request.Request(
        f"{kc}/admin/realms/{realm}/clients/{cid}/client-secret",
        headers={"Authorization": f"Bearer {admin_tok}", "User-Agent": UA},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        secret = json.loads(resp.read().decode()).get("value") or ""
    return _client_credentials(
        f"{kc}/realms/{realm}/protocol/openid-connect/token",
        client_id,
        secret,
    )


def resolve_token() -> str:
    bearer = _env("AM_MCP_BEARER_TOKEN")
    if bearer:
        print("auth: AM_MCP_BEARER_TOKEN", flush=True)
        return bearer

    token_file = ROOT / "dist" / "dev-mcp.token"
    if token_file.is_file():
        tok = token_file.read_text(encoding="utf-8").strip()
        if tok:
            print(f"auth: {token_file}", flush=True)
            return tok

    asrax = _asrax_env()
    cid = _env("AM_MCP_CLIENT_ID") or asrax.get("AM_MCP_CLIENT_ID", "")
    csec = _env("AM_MCP_CLIENT_SECRET") or asrax.get("AM_MCP_CLIENT_SECRET", "")
    token_url = _env("KEYCLOAK_TOKEN_URL") or asrax.get(
        "KEYCLOAK_TOKEN_URL",
        "https://auth.asrax.in/realms/am-dev-realm/protocol/openid-connect/token",
    )
    # Prefer am-dev-realm for DEV MCP JWKS
    if "am-realm" in token_url and "am-dev-realm" not in token_url:
        token_url = "https://auth.asrax.in/realms/am-dev-realm/protocol/openid-connect/token"
    if cid and csec:
        try:
            print(f"auth: client_credentials {cid} @ {token_url}", flush=True)
            return _client_credentials(token_url, cid, csec)
        except Exception as exc:  # noqa: BLE001
            print(f"auth: client_credentials failed: {exc}", flush=True)

    vault_addr = _env("VAULT_ADDR") or asrax.get("VAULT_ADDR", "")
    vault_token = _env("VAULT_TOKEN") or asrax.get("VAULT_TOKEN", "")
    if vault_addr and vault_token:
        print("auth: Keycloak admin via Vault am-identity", flush=True)
        return _token_via_keycloak_admin(vault_addr, vault_token)

    raise RuntimeError(
        "No DEV MCP token. Set AM_MCP_BEARER_TOKEN or Vault VAULT_TOKEN, "
        "or run scripts/_tmp_dev_auth.py"
    )


class McpSession:
    def __init__(self, base: str, token: str):
        self.base = base.rstrip("/")
        self.token = token
        self.session_id: str | None = None
        self.message_path = "/message"
        self.events: list[dict] = []
        self.lock = threading.Lock()
        self.ready = threading.Event()
        self._stop = False
        self._thread = threading.Thread(target=self._read_sse, daemon=True)
        self._thread.start()
        if not self.ready.wait(25):
            raise RuntimeError("SSE session not ready")
        self.post_rpc(
            "initialize",
            {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "mcp-sse-pilot-prove", "version": "0.2"},
            },
            rpc_id=1,
        )
        self._post_raw({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}})

    def _headers(self, extra: dict | None = None) -> dict:
        h = {"Authorization": f"Bearer {self.token}", "User-Agent": UA}
        if extra:
            h.update(extra)
        return h

    def _read_sse(self) -> None:
        # Public ingress: /mcp → filter rewrites to /sse
        sse_url = f"{self.base}/sse" if not self.base.endswith("/sse") else self.base
        if self.base.endswith("/mcp"):
            sse_url = f"{self.base}/sse"
        req = urllib.request.Request(
            sse_url,
            headers=self._headers({"Accept": "text/event-stream"}),
        )
        with urllib.request.urlopen(req, timeout=300) as resp:
            event = None
            data_lines: list[str] = []
            while not self._stop:
                line = resp.readline().decode("utf-8", errors="replace")
                if not line:
                    break
                line = line.rstrip("\n")
                if line.startswith("id:"):
                    self.session_id = line[3:].strip()
                elif line.startswith("event:"):
                    event = line[6:].strip()
                elif line.startswith("data:"):
                    data_lines.append(line[5:].lstrip())
                elif line == "":
                    if not data_lines:
                        continue
                    data = "\n".join(data_lines)
                    data_lines = []
                    if event == "endpoint":
                        path = data.strip() or "/message"
                        if path.startswith("http"):
                            from urllib.parse import urlparse

                            path = urlparse(path).path or "/message"
                        self.message_path = path
                        if self.session_id:
                            self.ready.set()
                    elif event == "message":
                        try:
                            payload = json.loads(data)
                        except json.JSONDecodeError:
                            payload = {"raw": data}
                        with self.lock:
                            self.events.append(payload)
                    event = None

    def _post_raw(self, body: dict) -> None:
        sep = "&" if "?" in self.message_path else "?"
        path = self.message_path
        if path.startswith("http"):
            url = f"{path}{sep}sessionId={self.session_id}"
        elif path.startswith("/mcp") and self.base.endswith("/mcp"):
            url = f"{self.base}{path[4:]}{sep}sessionId={self.session_id}"
        elif path.startswith("/"):
            # Prefer gateway /message (ingress) or /mcp/message (filter rewrite)
            if path == "/message" or path.startswith("/message?"):
                root = self.base[: -len("/mcp")] if self.base.endswith("/mcp") else self.base
                url = f"{root}{path}{sep}sessionId={self.session_id}"
            else:
                url = f"{self.base}{path}{sep}sessionId={self.session_id}"
        else:
            url = f"{self.base}/{path}{sep}sessionId={self.session_id}"
        req = urllib.request.Request(
            url,
            data=json.dumps(body).encode(),
            headers=self._headers({"Content-Type": "application/json"}),
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            resp.read()

    def post_rpc(self, method: str, params: dict | None, rpc_id: int) -> dict:
        self._post_raw(
            {"jsonrpc": "2.0", "id": rpc_id, "method": method, "params": params or {}}
        )
        deadline = time.time() + 90
        while time.time() < deadline:
            with self.lock:
                for i, ev in enumerate(self.events):
                    if ev.get("id") == rpc_id:
                        return self.events.pop(i)
            time.sleep(0.05)
        raise TimeoutError(f"No SSE response for {method} id={rpc_id}")

    def tools_list_full(self) -> list[dict]:
        resp = self.post_rpc("tools/list", {}, rpc_id=2)
        return list(resp.get("result", {}).get("tools", []) or [])

    def call_tool(self, name: str, arguments: dict, rpc_id: int) -> dict:
        resp = self.post_rpc(
            "tools/call", {"name": name, "arguments": arguments}, rpc_id=rpc_id
        )
        if "error" in resp:
            return {"ok": False, "error": resp["error"], "raw": resp}
        result = resp.get("result", resp)
        content = result.get("content") if isinstance(result, dict) else None
        text = ""
        if isinstance(content, list) and content:
            text = "\n".join(c.get("text", "") for c in content if isinstance(c, dict))
        else:
            text = json.dumps(result)[:2000]
        ok = True
        low = text.lower().replace(" ", "")
        if '"ok":false' in low or "iserror" in low:
            ok = False
        if isinstance(result, dict) and result.get("isError"):
            ok = False
        return {
            "ok": ok,
            "text": text[:800],
            "raw_keys": list(result) if isinstance(result, dict) else [],
        }


def _service_for_tool(name: str) -> str | None:
    n = name.lower()
    if n.startswith("api_am_market_data") or n.startswith("api_am_market-data"):
        return "am-market-data"
    if n.startswith("api_am_news"):
        return "am-news"
    if n.startswith("api_am_parser"):
        return "am-parser"
    market_domain = {
        "get_stock_quote",
        "search_instruments",
        "get_historical_data",
        "get_market_movers",
        "get_sector_performance",
        "get_indices_data",
    }
    if name in market_domain:
        return "am-market-data"
    return None


def _pick_contract_calls(
    tools: list[dict], service_id: str, limit: int | None = None
) -> list[tuple[str, dict]]:
    """All fillable GET api_* tools (limit=None = no cap). Prefer am-specs workflow for fixtures."""
    out: list[tuple[str, dict]] = []
    fixtures = {
        "symbol": "RELIANCE",
        "symbols": "RELIANCE",
        "query": "RELIANCE",
        "exchange": "NSE",
        "user_id": "user-demo-001",
        "userId": "user-demo-001",
        "job_id": "00000000-0000-0000-0000-000000000001",
        "schemeCode": "120503",
        "scheme_code": "120503",
        "limit": "5",
    }
    for t in tools:
        name = t.get("name") or ""
        if not name.startswith("api_"):
            continue
        if _service_for_tool(name) != service_id:
            continue
        desc = (t.get("description") or "").upper()
        schema = t.get("inputSchema") or {}
        props = schema.get("properties") or {}
        required = list(schema.get("required") or [])
        if any(m in desc for m in ("POST ", "PUT ", "DELETE ", "PATCH ")):
            continue
        args: dict = {}
        skip = False
        for r in required:
            if r in fixtures:
                args[r] = fixtures[r]
            elif r in props and "example" in (props.get(r) or {}):
                args[r] = props[r]["example"]
            elif r in props and "default" in (props.get(r) or {}):
                args[r] = props[r]["default"]
            else:
                skip = True
                break
        if skip:
            continue
        out.append((name, args))
        if limit is not None and len(out) >= limit:
            break
    return out


def render_html(report: dict) -> str:
    rows = []
    for r in report.get("calls") or []:
        cls = "ok" if r.get("ok") else ("miss" if r.get("missing") else "fail")
        rows.append(
            "<tr class='{c}'><td>{svc}</td><td>{tool}</td><td>{ok}</td><td>{detail}</td></tr>".format(
                c=cls,
                svc=html.escape(str(r.get("service"))),
                tool=html.escape(str(r.get("tool"))),
                ok=html.escape(str(r.get("ok"))),
                detail=html.escape(str(r.get("detail") or "")[:200]),
            )
        )
    by = report.get("by_service") or {}
    return f"""<!DOCTYPE html>
<html><head><meta charset="utf-8"/><title>MCP SSE pilot prove (DEV)</title>
<style>
body{{font-family:system-ui,sans-serif;margin:1.5rem}}
table{{border-collapse:collapse;width:100%;font-size:13px}}
th,td{{border:1px solid #ccc;padding:6px;text-align:left}}
tr.ok td{{background:#f0fff0}} tr.fail td{{background:#fff0f0}} tr.miss td{{background:#fff8e0}}
</style></head><body>
<h1>am-mcp-server SSE pilot prove (DEV)</h1>
<p>base=<code>{html.escape(str(report.get("base")))}</code>
 verdict=<b>{html.escape(str(report.get("verdict")))}</b>
 tools_listed={report.get("tools_listed")}</p>
<ul>
{"".join(f"<li>{html.escape(k)}: {html.escape(json.dumps(v))}</li>" for k, v in by.items())}
</ul>
<table><thead><tr><th>service</th><th>tool</th><th>ok</th><th>detail</th></tr></thead>
<tbody>{"".join(rows)}</tbody></table>
</body></html>"""


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument(
        "--base",
        default=_env("AM_MCP_BASE", "https://am-dev.asrax.in/mcp"),
    )
    ap.add_argument("--out-dir", type=Path, default=ROOT / "dist")
    ap.add_argument(
        "--contract-limit",
        type=int,
        default=0,
        help="Max api_* calls per pilot (0 = all fillable GET tools)",
    )
    args = ap.parse_args()

    if "preprod" in args.base or args.base.rstrip("/").endswith("am.asrax.in/mcp"):
        print("REFUSED: DEV-only prove (not preprod/prod)", file=sys.stderr)
        return 2

    token = resolve_token()
    print(f"auth_ok base={args.base}", flush=True)
    sess = McpSession(args.base, token)
    tools = sess.tools_list_full()
    names = sorted(t.get("name") or "" for t in tools)
    print(f"tools/list count={len(names)}", flush=True)

    by_list: dict[str, list[str]] = {p: [] for p in PILOTS}
    other: list[str] = []
    for n in names:
        svc = _service_for_tool(n)
        if svc:
            by_list[svc].append(n)
        else:
            other.append(n)

    calls: list[dict] = []
    rpc = 100

    for service_id, tool, arguments in DOMAIN_CASES:
        if tool not in names:
            calls.append(
                {
                    "service": service_id,
                    "tool": tool,
                    "ok": False,
                    "missing": True,
                    "detail": "not in tools/list",
                    "kind": "domain",
                }
            )
            continue
        rpc += 1
        try:
            s2 = McpSession(args.base, token)
            res = s2.call_tool(tool, arguments, rpc_id=rpc)
            calls.append(
                {
                    "service": service_id,
                    "tool": tool,
                    "ok": res.get("ok"),
                    "missing": False,
                    "detail": (res.get("text") or res.get("error") or "")[:300],
                    "kind": "domain",
                }
            )
            print(f"[{service_id}] domain {tool} -> {res.get('ok')}", flush=True)
        except Exception as exc:  # noqa: BLE001
            calls.append(
                {
                    "service": service_id,
                    "tool": tool,
                    "ok": False,
                    "missing": False,
                    "detail": str(exc)[:300],
                    "kind": "domain",
                }
            )
            print(f"[{service_id}] domain {tool} -> EXC {exc}", flush=True)

    for service_id in PILOTS:
        lim = None if args.contract_limit <= 0 else args.contract_limit
        picks = _pick_contract_calls(tools, service_id, limit=lim)
        if not picks:
            calls.append(
                {
                    "service": service_id,
                    "tool": "(contract)",
                    "ok": False,
                    "missing": True,
                    "detail": "no api_* contract tools — deploy image with packs + AM_CONTRACT_MCP_ENABLED",
                    "kind": "contract",
                }
            )
            print(f"[{service_id}] no contract tools registered", flush=True)
            continue
        for tool, arguments in picks:
            rpc += 1
            try:
                s2 = McpSession(args.base, token)
                res = s2.call_tool(tool, arguments, rpc_id=rpc)
                calls.append(
                    {
                        "service": service_id,
                        "tool": tool,
                        "ok": res.get("ok"),
                        "missing": False,
                        "detail": (res.get("text") or "")[:300],
                        "kind": "contract",
                        "args": arguments,
                    }
                )
                print(f"[{service_id}] contract {tool} -> {res.get('ok')}", flush=True)
            except Exception as exc:  # noqa: BLE001
                calls.append(
                    {
                        "service": service_id,
                        "tool": tool,
                        "ok": False,
                        "detail": str(exc)[:300],
                        "kind": "contract",
                    }
                )
                print(f"[{service_id}] contract {tool} -> EXC {exc}", flush=True)

    by_service: dict[str, dict] = {}
    for p in PILOTS:
        rows = [c for c in calls if c.get("service") == p]
        invoked = [c for c in rows if not c.get("missing")]
        passed = [c for c in invoked if c.get("ok")]
        by_service[p] = {
            "listed_tools": len(by_list.get(p) or []),
            "calls": len(rows),
            "invoked": len(invoked),
            "passed": len(passed),
            "missing_contract": any(
                c.get("missing") and c.get("kind") == "contract" for c in rows
            ),
        }

    # Align with SpecKit workflow: >=5 passes and fail_ratio <= 0.50 per pilot
    def _svc_go(row: dict) -> bool:
        inv = int(row.get("invoked") or 0)
        passed = int(row.get("passed") or 0)
        if inv <= 0:
            return False
        fail_ratio = (inv - passed) / inv
        return passed >= 5 and fail_ratio <= 0.50

    for p in PILOTS:
        row = by_service[p]
        inv = int(row.get("invoked") or 0)
        passed = int(row.get("passed") or 0)
        row["fail_ratio"] = round(((inv - passed) / inv) if inv else 1.0, 3)
        row["verdict"] = "go" if _svc_go(row) else "no_go"

    verdict = (
        "go"
        if all(by_service[p].get("verdict") == "go" for p in PILOTS)
        else "no_go"
    )

    report = {
        "base": args.base,
        "env": "dev",
        "tools_listed": len(names),
        "tools_by_pilot": {k: v for k, v in by_list.items()},
        "other_tool_count": len(other),
        "sample_other_tools": other[:20],
        "by_service": by_service,
        "calls": calls,
        "verdict": verdict,
        "ok": verdict == "go",
    }

    out = args.out_dir
    out.mkdir(parents=True, exist_ok=True)
    jp = out / "mcp-sse-pilot-report.json"
    hp = out / "mcp-sse-pilot-report.html"
    jp.write_text(json.dumps(report, indent=2), encoding="utf-8")
    hp.write_text(render_html(report), encoding="utf-8")
    print(json.dumps({"verdict": verdict, "by_service": by_service}, indent=2), flush=True)
    print(f"wrote {jp}", flush=True)
    print(f"wrote {hp}", flush=True)
    return 0 if verdict == "go" else 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        print("HTTPError", e.code, e.read()[:500], file=sys.stderr)
        raise SystemExit(1)
    except Exception as e:
        print("FATAL", e, file=sys.stderr)
        raise SystemExit(1)
