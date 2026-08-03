#!/usr/bin/env python3
"""List all MCP tools from am-mcp-server over SSE.

Usage (from services/am-mcp-server):
  npm run list-tools
  npm run list-tools:json

Loads `.env` from the service root (same folder as package.json). Auth keys:
  AM_TOKEN, or AM_AUTH_USER + AM_AUTH_PASS (+ optional AM_AUTH_URL / AM_MCP_BASE).
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

UA = "Mozilla/5.0 (compatible; am-mcp-list-tools/1.0)"
SERVICE_ROOT = Path(__file__).resolve().parents[1]


def _load_dotenv() -> None:
    """Load KEY=VALUE from service `.env` without overriding existing env vars."""
    path = SERVICE_ROOT / ".env"
    if not path.is_file():
        return
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        if not key or key in os.environ:
            continue
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        os.environ[key] = value


def _env(name: str, default: str = "") -> str:
    return (os.environ.get(name) or default).strip()


def login(auth_url: str, username: str, password: str) -> str:
    body = json.dumps({"username": username, "password": password}).encode()
    req = urllib.request.Request(
        auth_url.rstrip("/") + "/auth/login"
        if not auth_url.rstrip("/").endswith("/auth/login")
        else auth_url,
        data=body,
        headers={"Content-Type": "application/json", "User-Agent": UA},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read())
    token = data.get("access_token")
    if not token:
        raise RuntimeError("login response missing access_token")
    return token


class McpSession:
    def __init__(self, base: str, token: str) -> None:
        self.base = base.rstrip("/")
        self.token = token
        self.session_id: str | None = None
        self.message_path = "/message"
        self.events: list[dict] = []
        self.lock = threading.Lock()
        self.ready = threading.Event()
        self._err: BaseException | None = None
        threading.Thread(target=self._read_sse, daemon=True).start()
        if not self.ready.wait(30):
            raise RuntimeError(f"SSE session not ready: {self._err}")
        self.post_rpc(
            "initialize",
            {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "am-mcp-list-tools", "version": "1.0"},
            },
            rpc_id=1,
        )
        self._post_raw({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}})

    def _headers(self, extra: dict | None = None) -> dict[str, str]:
        h = {"Authorization": f"Bearer {self.token}", "User-Agent": UA}
        if extra:
            h.update(extra)
        return h

    def _read_sse(self) -> None:
        try:
            req = urllib.request.Request(
                f"{self.base}/sse",
                headers=self._headers({"Accept": "text/event-stream"}),
            )
            with urllib.request.urlopen(req, timeout=180) as resp:
                event: str | None = None
                data_lines: list[str] = []
                while True:
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
                        data = "\n".join(data_lines)
                        data_lines = []
                        if event == "endpoint" and self.session_id:
                            path = (data.strip() or "/message").split("?", 1)[0]
                            self.message_path = path if path.startswith("/") else f"/{path}"
                            self.ready.set()
                        elif event == "message":
                            try:
                                payload = json.loads(data)
                            except json.JSONDecodeError:
                                payload = {"raw": data}
                            with self.lock:
                                self.events.append(payload)
                        event = None
        except BaseException as exc:  # noqa: BLE001
            self._err = exc

    def _post_raw(self, body: dict) -> None:
        url = f"{self.base}{self.message_path}?sessionId={self.session_id}"
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

    def list_tools(self) -> list[dict]:
        resp = self.post_rpc("tools/list", {}, rpc_id=2)
        if "error" in resp:
            raise RuntimeError(f"tools/list error: {resp['error']}")
        return list(resp.get("result", {}).get("tools", []) or [])


def main() -> int:
    _load_dotenv()

    parser = argparse.ArgumentParser(description="List am-mcp-server tools via SSE")
    parser.add_argument(
        "--base",
        default=_env("AM_MCP_BASE", "https://am-dev.asrax.in/mcp"),
        help="MCP base URL (no trailing /sse)",
    )
    parser.add_argument("--token", default=_env("AM_TOKEN"), help="Bearer JWT")
    parser.add_argument(
        "--auth-url",
        default=_env("AM_AUTH_URL", "https://am-dev.asrax.in/identity"),
        help="Identity base or .../auth/login",
    )
    parser.add_argument("--user", default=_env("AM_AUTH_USER"))
    parser.add_argument("--password", default=_env("AM_AUTH_PASS"))
    parser.add_argument("--json", action="store_true", help="Print full tools JSON")
    args = parser.parse_args()

    token = args.token
    if not token:
        if not args.user or not args.password:
            print(
                "Add to .env: AM_AUTH_USER + AM_AUTH_PASS (or AM_TOKEN). "
                f"Looked for {SERVICE_ROOT / '.env'}",
                file=sys.stderr,
            )
            return 2
        try:
            token = login(args.auth_url, args.user, args.password)
        except Exception as exc:  # noqa: BLE001
            print(f"login failed: {exc}", file=sys.stderr)
            return 1

    try:
        session = McpSession(args.base, token)
        tools = session.list_tools()
    except urllib.error.HTTPError as exc:
        body = exc.read()[:400]
        print(f"HTTP {exc.code}: {body!r}", file=sys.stderr)
        return 1
    except Exception as exc:  # noqa: BLE001
        print(f"failed: {exc}", file=sys.stderr)
        return 1

    if args.json:
        print(json.dumps({"base": args.base, "count": len(tools), "tools": tools}, indent=2))
        return 0

    print(f"base={args.base}")
    print(f"session_id={session.session_id}")
    print(f"count={len(tools)}")
    for tool in sorted(tools, key=lambda t: t.get("name") or ""):
        name = tool.get("name") or ""
        desc = (tool.get("description") or "").replace("\n", " ").strip()
        if len(desc) > 100:
            desc = desc[:97] + "..."
        print(f"- {name}: {desc}" if desc else f"- {name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
