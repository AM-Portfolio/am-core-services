#!/usr/bin/env python3
"""Launch a remote MCP (SSE) session for any MCP-capable client.

Bridges local stdio <-> remote HTTP/SSE so ChatGPT, Claude, Cursor, and other
LLM tools can attach to am-mcp-server the same way.

Auth (prefer Keycloak client_credentials — same as platform agents):
  AM_MCP_CLIENT_ID / AM_MCP_CLIENT_SECRET + KEYCLOAK_TOKEN_URL
Fallback (human / break-glass):
  AM_AUTH_USER / AM_AUTH_PASS → AM_AUTH_LOGIN_URL (am-identity)

Env:
  AM_MCP_SSE_URL      default https://am-dev.asrax.in/mcp/sse
  KEYCLOAK_TOKEN_URL  default https://auth.asrax.in/auth/realms/am-dev-realm/protocol/openid-connect/token
  AM_AUTH_LOGIN_URL   default https://am-dev.asrax.in/identity/auth/login
  AM_MCP_PF_PORT / AM_IDENTITY_PF_PORT / KUBECONFIG — only for localhost SSE
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
import urllib.parse
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


def _client_credentials(token_url: str, client_id: str, client_secret: str) -> str:
    body = urllib.parse.urlencode(
        {
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
        }
    ).encode()
    req = urllib.request.Request(
        token_url,
        data=body,
        headers={
            "Content-Type": "application/x-www-form-urlencoded",
            "Accept": "application/json",
            "User-Agent": "am-mcp-remote-launch/1.1",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode())
    token = data.get("access_token") or ""
    if not token:
        raise RuntimeError(f"client_credentials ok but no access_token: keys={list(data)}")
    return token


def _login(url: str, user: str, password: str) -> str:
    body = json.dumps({"username": user, "password": password}).encode()
    req = urllib.request.Request(
        url,
        data=body,
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "am-mcp-remote-launch/1.1",
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


def _resolve_token(
    *,
    token_url: str,
    client_id: str,
    client_secret: str,
    login_url: str,
    user: str,
    password: str,
    identity_port: int,
) -> str:
    if client_id and client_secret:
        try:
            print("auth: Keycloak client_credentials", file=sys.stderr)
            return _client_credentials(token_url, client_id, client_secret)
        except Exception as exc:  # noqa: BLE001
            print(f"client_credentials failed: {exc}", file=sys.stderr)
            if not (user and password):
                raise
            print("auth: falling back to identity user login", file=sys.stderr)
    if user and password:
        return _login_with_fallback(login_url, user, password, identity_port)
    raise RuntimeError(
        "Set AM_MCP_CLIENT_ID+AM_MCP_CLIENT_SECRET (preferred) "
        "or AM_AUTH_USER+AM_AUTH_PASS"
    )


def main() -> int:
    local_port = int(os.environ.get("AM_MCP_PF_PORT", "18080"))
    identity_port = int(os.environ.get("AM_IDENTITY_PF_PORT", "18001"))
    sse_url = os.environ.get("AM_MCP_SSE_URL", "https://am-dev.asrax.in/mcp/sse")
    token_url = os.environ.get(
        "KEYCLOAK_TOKEN_URL",
        "https://auth.asrax.in/auth/realms/am-dev-realm/protocol/openid-connect/token",
    )
    login_url = os.environ.get(
        "AM_AUTH_LOGIN_URL", "https://am-dev.asrax.in/identity/auth/login"
    )
    client_id = os.environ.get("AM_MCP_CLIENT_ID", "").strip()
    client_secret = os.environ.get("AM_MCP_CLIENT_SECRET", "").strip()
    user = os.environ.get("AM_AUTH_USER", "").strip()
    password = os.environ.get("AM_AUTH_PASS", "").strip()

    if sse_url.startswith("http://127.0.0.1") or sse_url.startswith("http://localhost"):
        try:
            _ensure_port_forward(local_port, "am-mcp-server")
        except Exception as exc:  # noqa: BLE001
            print(f"port-forward failed: {exc}", file=sys.stderr)
            return 1

    try:
        token = _resolve_token(
            token_url=token_url,
            client_id=client_id,
            client_secret=client_secret,
            login_url=login_url,
            user=user,
            password=password,
            identity_port=identity_port,
        )
    except Exception as exc:  # noqa: BLE001
        print(f"auth failed: {exc}", file=sys.stderr)
        return 1

    npx = shutil.which("npx.cmd") or shutil.which("npx")
    if not npx:
        print("npx not found on PATH", file=sys.stderr)
        return 1

    # Cursor/Windows: no spaces in --header argv (npx.cmd mangles them).
    # mcp-remote expands ${MCP_REMOTE_AUTH} after parse.
    env = os.environ.copy()
    env["MCP_REMOTE_AUTH"] = f"Bearer {token}"
    cmd = [
        npx,
        "-y",
        "mcp-remote",
        sse_url,
        "--transport",
        "sse-only",
        "--header",
        "Authorization:${MCP_REMOTE_AUTH}",
        "--header",
        "User-Agent:am-asrax-mcp",
    ]
    if sse_url.startswith("http://"):
        cmd.append("--allow-http")
    return subprocess.call(cmd, env=env)


if __name__ == "__main__":
    raise SystemExit(main())
