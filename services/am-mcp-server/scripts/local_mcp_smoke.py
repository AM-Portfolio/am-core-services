#!/usr/bin/env python3
"""Local smoke test for am-mcp-server SSE MCP (fresh session per call)."""
from __future__ import annotations

import json
import sys
import threading
import time
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:18080"
AUTH_URL = "http://127.0.0.1:8001/auth/login"


def login() -> str:
    body = json.dumps(
        {"username": "ssd2658@gmail.com", "password": "@M1unish"}
    ).encode()
    req = urllib.request.Request(
        AUTH_URL, data=body, headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = json.loads(resp.read())
    return data["access_token"]


class McpSession:
    def __init__(self, token: str):
        self.token = token
        self.session_id: str | None = None
        self.message_path = "/message"
        self.events: list[dict] = []
        self.lock = threading.Lock()
        self.ready = threading.Event()
        self._stop = False
        self._thread = threading.Thread(target=self._read_sse, daemon=True)
        self._thread.start()
        if not self.ready.wait(20):
            raise RuntimeError("SSE session not ready")
        self.post_rpc(
            "initialize",
            {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "local-smoke", "version": "0.1"},
            },
            rpc_id=1,
        )
        self._post_raw({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}})

    def _headers(self, extra: dict | None = None) -> dict:
        h = {"Authorization": f"Bearer {self.token}"}
        if extra:
            h.update(extra)
        return h

    def _read_sse(self):
        req = urllib.request.Request(
            f"{BASE}/sse",
            headers=self._headers({"Accept": "text/event-stream"}),
        )
        with urllib.request.urlopen(req, timeout=180) as resp:
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
                        self.message_path = data.strip() or "/message"
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

    def _post_raw(self, body: dict):
        sep = "&" if "?" in self.message_path else "?"
        url = f"{BASE}{self.message_path}{sep}sessionId={self.session_id}"
        req = urllib.request.Request(
            url,
            data=json.dumps(body).encode(),
            headers=self._headers({"Content-Type": "application/json"}),
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            resp.read()

    def post_rpc(self, method: str, params: dict | None, rpc_id: int) -> dict:
        self._post_raw(
            {"jsonrpc": "2.0", "id": rpc_id, "method": method, "params": params or {}}
        )
        deadline = time.time() + 60
        while time.time() < deadline:
            with self.lock:
                for i, ev in enumerate(self.events):
                    if ev.get("id") == rpc_id:
                        return self.events.pop(i)
            time.sleep(0.05)
        raise TimeoutError(f"No SSE response for {method} id={rpc_id}")

    def tools_list(self) -> list[str]:
        resp = self.post_rpc("tools/list", {}, rpc_id=2)
        tools = resp.get("result", {}).get("tools", [])
        return sorted(t["name"] for t in tools)

    def call_tool(self, name: str, arguments: dict, rpc_id: int = 10) -> str:
        resp = self.post_rpc(
            "tools/call", {"name": name, "arguments": arguments}, rpc_id=rpc_id
        )
        result = resp.get("result", resp)
        content = result.get("content") if isinstance(result, dict) else None
        if isinstance(content, list) and content:
            return "\n".join(
                c.get("text", "") for c in content if isinstance(c, dict)
            )
        return json.dumps(result)[:1000]


def classify(text: str) -> str:
    low = text.lower()
    if '"ok":false' in low.replace(" ", "") or '"ok": false' in low:
        return "ERR_ENVELOPE"
    if "unavailable" in low or "connect" in low and "refused" in low:
        return "DOWNSTREAM_DOWN"
    if text.strip().startswith("{") or text.strip().startswith("["):
        return "OK_JSON"
    if len(text) > 20:
        return "OK_TEXT"
    return "UNKNOWN"


def main() -> int:
    token = login()
    print(f"base={BASE} auth_ok")

    s = McpSession(token)
    names = s.tools_list()
    print(f"tools/list count={len(names)}")
    for n in names:
        print(f"  - {n}")
    if len(names) != 28:
        print(f"FAIL expected 28 tools, got {len(names)}")
        return 2

    cases = [
        ("portfolio", "get_portfolio_summary", {}),
        ("market", "search_instruments", {"query": "RELIANCE"}),
        ("market", "get_stock_quote", {"symbol": "RELIANCE"}),
        ("trade", "get_recent_activity", {"userId": "user1"}),
        ("basket", "get_basket_opportunities", {"userId": "user1"}),
        ("analysis", "get_top_movers", {}),
    ]

    results = []
    for i, (domain, tool, args) in enumerate(cases):
        try:
            # Fresh session avoids SSE recycle issues across long calls
            sess = McpSession(token)
            text = sess.call_tool(tool, args, rpc_id=100 + i)
            kind = classify(text)
            print(f"\n[{domain}] {tool} -> {kind}")
            print(text[:400])
            results.append((tool, kind, True))
        except Exception as e:
            print(f"\n[{domain}] {tool} -> EXCEPTION {e}")
            results.append((tool, "EXCEPTION", False))

    reachable = sum(1 for _, kind, ok in results if ok)
    print(f"\nSUMMARY tools=28 reachable={reachable}/{len(results)}")
    # Gate for local: all 28 tools registered + majority of calls return (even error envelopes)
    return 0 if reachable >= 4 else 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except urllib.error.HTTPError as e:
        print("HTTPError", e.code, e.read()[:500], file=sys.stderr)
        raise SystemExit(1)
    except Exception as e:
        print("FATAL", e, file=sys.stderr)
        raise SystemExit(1)
