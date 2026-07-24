#!/usr/bin/env python3
"""Launch a remote MCP (SSE) session for any MCP-capable client.

Bridges local stdio <-> remote HTTP/SSE so ChatGPT, Claude, Cursor, and other
LLM tools can attach to am-mcp-server the same way.

Env:
  AM_MCP_SSE_URL    default http://127.0.0.1:18080/sse (kubectl port-forward)
                    public: https://am-dev.asrax.in/mcp/sse
  AM_AUTH_LOGIN_URL default https://am-dev.asrax.in/identity/auth/login
                    (falls back to identity port-forward if public login fails)
  AM_AUTH_USER / AM_AUTH_PASS  identity credentials (Bearer for /sse)
  AM_MCP_PF_PORT    local port for am-mcp-server forward (default 18080)
  AM_IDENTITY_PF_PORT local port for am-identity forward (default 18001)
  KUBECONFIG        required for auto port-forward when using localhost SSE
"""

from __future__ import annotations

import json
import os
import shutil
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request


def _port_open(host: str, port: int) -> bool:
    s = socket.socket()
    s.settimeout(0.5)
    try:
        s.connect((host, port))
        return True
    except OSError:
        return False
    finally:
        s.close()


def _ensure_port_forward(local_port: int, service: str) -> None:
    if _port_open("127.0.0.1", local_port):
        return
    kube = shutil.which("kubectl")
    if not kube:
        raise RuntimeError("kubectl not found; start port-forward manually")
    env = os.environ.copy()
    if "KUBECONFIG" not in env:
        default = os.path.expanduser(r"~\.am\kubeconfig.vps")
        if os.path.isfile(default):
            env["KUBECONFIG"] = default
    subprocess.Popen(
        [
            kube,
            "port-forward",
            "-n",
            "am-apps-dev",
            f"svc/{service}",
            f"{local_port}:8080",
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        env=env,
    )
    for _ in range(30):
        time.sleep(0.3)
        if _port_open("127.0.0.1", local_port):
            return
    raise RuntimeError(f"port-forward to 127.0.0.1:{local_port} ({service}) did not come up")


def _login(url: str, user: str, password: str) -> str:
    body = json.dumps({"username": user, "password": password}).encode()
    req = urllib.request.Request(
        url,
        data=body,
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "am-mcp-remote-launch/1.0",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode())
    token = data.get("access_token") or ""
    if not token:
        raise RuntimeError(f"login ok but no access_token: keys={list(data)}")
    return token


def _login_with_fallback(login_url: str, user: str, password: str, identity_port: int) -> str:
    try:
        return _login(login_url, user, password)
    except Exception as exc:  # noqa: BLE001
        print(f"identity login failed ({login_url}): {exc}", file=sys.stderr)
        fallback = f"http://127.0.0.1:{identity_port}/auth/login"
        if login_url.rstrip("/") == fallback.rstrip("/"):
            raise
        print(f"trying identity via port-forward {fallback}", file=sys.stderr)
        _ensure_port_forward(identity_port, "am-identity")
        return _login(fallback, user, password)


def main() -> int:
    local_port = int(os.environ.get("AM_MCP_PF_PORT", "18080"))
    identity_port = int(os.environ.get("AM_IDENTITY_PF_PORT", "18001"))
    sse_url = os.environ.get("AM_MCP_SSE_URL", f"http://127.0.0.1:{local_port}/sse")
    login_url = os.environ.get(
        "AM_AUTH_LOGIN_URL", "https://am-dev.asrax.in/identity/auth/login"
    )
    user = os.environ.get("AM_AUTH_USER", "")
    password = os.environ.get("AM_AUTH_PASS", "")
    if not user or not password:
        print("AM_AUTH_USER and AM_AUTH_PASS are required", file=sys.stderr)
        return 2

    if sse_url.startswith("http://127.0.0.1") or sse_url.startswith("http://localhost"):
        try:
            _ensure_port_forward(local_port, "am-mcp-server")
        except Exception as exc:  # noqa: BLE001
            print(f"port-forward failed: {exc}", file=sys.stderr)
            return 1

    try:
        token = _login_with_fallback(login_url, user, password, identity_port)
    except Exception as exc:  # noqa: BLE001
        print(f"identity login failed: {exc}", file=sys.stderr)
        return 1

    npx = shutil.which("npx.cmd") or shutil.which("npx")
    if not npx:
        print("npx not found on PATH", file=sys.stderr)
        return 1

    # Cursor/Windows: no spaces in --header args (mangled otherwise).
    # Force SSE — http-first tries OAuth /register and fails against am-mcp-server.
    cmd = [
        npx,
        "-y",
        "mcp-remote",
        sse_url,
        "--transport",
        "sse-only",
        "--header",
        f"Authorization:Bearer {token}",
    ]
    if sse_url.startswith("http://"):
        cmd.append("--allow-http")
    return subprocess.call(cmd)


if __name__ == "__main__":
    raise SystemExit(main())
