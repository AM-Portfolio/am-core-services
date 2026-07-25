#!/usr/bin/env python3
"""CI helpers for .github/workflows/qa-agent-notify.yml (no third-party deps)."""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


def load_manifest(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")
    data: dict = {}
    for key in ("enabled", "environment", "service"):
        m = re.search(rf"(?m)^{key}:\s*(.+?)\s*$", text)
        if not m:
            continue
        val = m.group(1).strip().strip("\"'")
        data[key] = val.lower() in ("true", "yes", "1") if key == "enabled" else val
    bm = re.search(r"(?m)^branches:\s*\[([^\]]*)\]\s*$", text)
    data["branches"] = (
        [b.strip().strip("\"'") for b in bm.group(1).split(",") if b.strip()] if bm else []
    )
    sm = re.search(r"(?m)^spt:\s*\n(?:  .+\n)*", text)
    if sm:
        rm = re.search(r"(?m)^  required:\s*(.+?)\s*$", sm.group(0))
        if rm:
            data["spt_required"] = rm.group(1).strip().strip("\"'").lower() in ("true", "yes", "1")
    if "spt_required" not in data:
        rm2 = re.search(r"(?m)^require_spt_catalog:\s*(.+?)\s*$", text)
        data["spt_required"] = (
            rm2.group(1).strip().strip("\"'").lower() in ("true", "yes", "1") if rm2 else True
        )
    return data


def cmd_detect() -> int:
    branch = os.environ["BRANCH"]
    input_svc = (os.environ.get("INPUT_SERVICE") or "").strip()
    candidates = ["am-analysis", "am-gateway", "am-mcp-server"]

    def enabled(svc: str):
        man = Path(f"services/{svc}/qa-agent.yaml")
        if not man.is_file():
            return False, "missing_manifest", "dev", True
        data = load_manifest(man)
        if not data.get("enabled", False):
            return False, "disabled", str(data.get("environment") or "dev"), True
        branches = data.get("branches") or []
        if branches and branch not in branches:
            return False, f"branch_not_listed:{branch}", str(data.get("environment") or "dev"), True
        req = bool(data.get("spt_required", True))
        if req and not Path(f"services/{svc}/spt.yaml").is_file():
            return False, "missing_spt_yaml", str(data.get("environment") or "dev"), True
        return True, "ok", str(data.get("environment") or "dev"), req

    service = input_svc
    if not service and os.environ.get("GITHUB_EVENT_NAME") == "push":
        try:
            diff = subprocess.check_output(
                ["git", "diff", "--name-only", "HEAD~1", "HEAD"], text=True
            )
        except subprocess.CalledProcessError:
            diff = ""
        for line in diff.splitlines():
            for c in candidates:
                if line.startswith(f"services/{c}/"):
                    service = c
                    break
            if service:
                break
    if not service:
        service = "am-analysis"

    ok, reason, env, req = enabled(service)
    print(f"service={service}")
    print(f"environment={env}")
    print(f"require_catalog={'true' if req else 'false'}")
    print("skip=false" if ok else "skip=true")
    print(f"reason={'ok' if ok else reason}")
    return 0


def cmd_wait() -> int:
    base = os.environ["QA_AGENT_BASE_URL"].rstrip("/")
    service = os.environ["SERVICE"]
    url = f"{base}/api/catalog/registrations"
    print(f"Waiting for {service} in {url}")
    for i in range(1, 37):
        try:
            with urllib.request.urlopen(url, timeout=20) as resp:
                code = resp.status
                body = resp.read().decode("utf-8", errors="replace")
        except urllib.error.HTTPError as e:
            code = e.code
            body = e.read().decode("utf-8", errors="replace")
        except Exception as e:
            print(f"attempt {i}: error {e}")
            time.sleep(5)
            continue
        ready = False
        if code == 200:
            try:
                data = json.loads(body)
                ids = [x.get("id") or x.get("service") or "" for x in (data.get("services") or [])]
                ready = service in ids
            except json.JSONDecodeError:
                ready = False
        if ready:
            print(f"Catalog ready for {service} (attempt {i})")
            print(body)
            return 0
        print(f"attempt {i}: http={code} not ready yet")
        time.sleep(5)
    print(f"::error::catalog_not_ready for {service}", file=sys.stderr)
    return 1


def cmd_notify() -> int:
    base = os.environ["QA_AGENT_BASE_URL"].rstrip("/")
    token = os.environ["QA_AGENT_GATEWAY_TOKEN"]
    payload = {
        "repo": os.environ["REPO"],
        "branch": os.environ["BRANCH"],
        "head_sha": os.environ["SHA"],
        "service": os.environ["SERVICE"],
        "ci_conclusion": "success",
        "trigger_kind": "ci_master_merge",
        "environment": os.environ["ENV"],
        "assume_ci_success": True,
        "use_temporal": True,
    }
    url = f"{base}/v2/workflows/release-readiness"
    print(f"POST {url}")
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            code = resp.status
            body = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        code = e.code
        body = e.read().decode("utf-8", errors="replace")
    print(f"HTTP {code}")
    print(body)
    if code < 200 or code >= 300:
        print(f"::error::qa-agent notify failed with HTTP {code}", file=sys.stderr)
        return 1
    try:
        data = json.loads(body)
    except json.JSONDecodeError:
        data = {}
    if data.get("skipped") is True:
        print(f"::warning::qa-agent skipped: {data.get('reason')}")
        return 0
    print(f"qa-agent activated for {os.environ['SERVICE']} on {os.environ['BRANCH']}")
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: qa_agent_notify_ci.py detect|wait|notify", file=sys.stderr)
        return 2
    cmd = sys.argv[1]
    if cmd == "detect":
        return cmd_detect()
    if cmd == "wait":
        return cmd_wait()
    if cmd == "notify":
        return cmd_notify()
    print(f"unknown command: {cmd}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
