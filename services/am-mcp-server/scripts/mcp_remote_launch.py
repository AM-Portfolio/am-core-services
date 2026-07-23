#!/usr/bin/env python3
"""Launch a remote MCP (SSE) session for any MCP-capable client.

Bridges local stdio <-> remote HTTP/SSE so ChatGPT, Claude, Cursor, and other
LLM tools can attach to am-mcp-server the same way.

Env:
  AM_MCP_SSE_URL    default https://am-dev.asrax.in/mcp/sse
                    (or http://127.0.0.1:18080/sse with kubectl port-forward)
  AM_AUTH_LOGIN_URL default https://am-dev.asrax.in/auth/login
  AM_AUTH_USER / AM_AUTH_PASS  identity credentials (Bearer for /sse)
"""

from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import urllib.request


def _login(url: str, user: str, password: str) -> str:
    body = json.dumps({"username": user, "password": password}).encode()
    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode())
    token = data.get("access_token") or ""
    if not token:
        raise RuntimeError(f"login ok but no access_token: keys={list(data)}")
    return token


def main() -> int:
    sse_url = os.environ.get("AM_MCP_SSE_URL", "https://am-dev.asrax.in/mcp/sse")
    login_url = os.environ.get(
        "AM_AUTH_LOGIN_URL", "https://am-dev.asrax.in/auth/login"
    )
    user = os.environ.get("AM_AUTH_USER", "")
    password = os.environ.get("AM_AUTH_PASS", "")
    if not user or not password:
        print("AM_AUTH_USER and AM_AUTH_PASS are required", file=sys.stderr)
        return 2

    try:
        token = _login(login_url, user, password)
    except Exception as exc:  # noqa: BLE001
        print(f"identity login failed: {exc}", file=sys.stderr)
        return 1

    npx = shutil.which("npx.cmd") or shutil.which("npx")
    if not npx:
        print("npx not found on PATH", file=sys.stderr)
        return 1

    # mcp-remote: local stdio <-> remote SSE/HTTP MCP (client-agnostic)
    cmd = [
        npx,
        "-y",
        "mcp-remote",
        sse_url,
        "--header",
        f"Authorization:Bearer {token}",
    ]
    return subprocess.call(cmd)


if __name__ == "__main__":
    raise SystemExit(main())
