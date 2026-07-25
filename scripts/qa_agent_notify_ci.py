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

    matched: list[str] = []
    if input_svc:
        matched = [input_svc]
    elif os.environ.get("GITHUB_EVENT_NAME") == "push":
        try:
            diff = subprocess.check_output(
                ["git", "diff", "--name-only", "HEAD~1", "HEAD"], text=True
            )
        except subprocess.CalledProcessError:
            diff = ""
        for line in diff.splitlines():
            for c in candidates:
                if line.startswith(f"services/{c}/") and c not in matched:
                    matched.append(c)
    if not matched:
        matched = ["am-analysis"]

    # Primary = first matched (workflow jobs use single service today)
    service = matched[0]
    ok, reason, env, req = enabled(service)
    # Also emit all enabled matched services for future fan-out
    enabled_services = [s for s in matched if enabled(s)[0]]
    print(f"service={service}")
    print(f"services={','.join(enabled_services) if enabled_services else service}")
    print(f"environment={env}")
    print(f"require_catalog={'true' if req else 'false'}")
    print("skip=false" if ok else "skip=true")
    print(f"reason={'ok' if ok else reason}")
    return 0


def _http_json(url: str, *, method: str = "GET", data: dict | None = None, token: str | None = None, timeout: int = 30):
    headers = {"Accept": "application/json"}
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")


def cmd_wait() -> int:
    base = os.environ["QA_AGENT_BASE_URL"].rstrip("/")
    service = os.environ["SERVICE"]
    url = f"{base}/api/catalog/registrations"
    print(f"Waiting for {service} in {url}")
    for i in range(1, 37):
        try:
            code, body = _http_json(url, timeout=10)
        except Exception as e:
            print(f"attempt {i}: error {type(e).__name__}: {e}")
            time.sleep(5)
            continue
        ready = False
        ids: list[str] = []
        if code == 200:
            try:
                data = json.loads(body)
                ids = [x.get("id") or x.get("service") or "" for x in (data.get("services") or [])]
                ready = service in ids
            except json.JSONDecodeError as e:
                print(f"attempt {i}: bad json: {e}")
                ready = False
        if ready:
            print(f"Catalog ready for {service} (attempt {i})")
            print(body[:2000])
            return 0
        print(f"attempt {i}: http={code} ids={ids!r} not ready yet")
        time.sleep(5)
    print(f"::error::catalog_not_ready for {service}", file=sys.stderr)
    return 1


def cmd_notify() -> int:
    base = os.environ["QA_AGENT_BASE_URL"].rstrip("/")
    token = os.environ["QA_AGENT_GATEWAY_TOKEN"]
    # Fail fast if Specs cannot run k6 (release SPT would soft-fail / miss evidence)
    try:
        hc, hb = _http_json(f"{base}/api/platform/health", timeout=10)
        if hc == 200:
            health = json.loads(hb)
            if health.get("k6_binary") is False:
                print("::error::qa-agent k6_binary=false — rebuild image with k6 before notify", file=sys.stderr)
                return 1
    except Exception as e:
        print(f"::warning::platform health check failed: {e}")

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
    try:
        code, body = _http_json(url, method="POST", data=payload, token=token, timeout=60)
    except Exception as e:
        print(f"::error::qa-agent notify request failed: {e}", file=sys.stderr)
        return 1
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
    tid = data.get("tracking_id") or ""
    print(f"tracking_id={tid}")
    gh_out = os.environ.get("GITHUB_OUTPUT")
    if gh_out and tid:
        with open(gh_out, "a", encoding="utf-8") as f:
            f.write(f"tracking_id={tid}\n")
    print(f"qa-agent activated for {os.environ['SERVICE']} on {os.environ['BRANCH']}")
    return 0


def cmd_wait_verify() -> int:
    """Poll until Temporal verify finishes (awaiting_release step), then fail if not releasable.

    Does not wait for HITL approve/reject.
    """
    base = os.environ["QA_AGENT_BASE_URL"].rstrip("/")
    token = os.environ.get("QA_AGENT_GATEWAY_TOKEN") or ""
    tid = (os.environ.get("TRACKING_ID") or "").strip()
    if not tid:
        print("::error::TRACKING_ID required for wait-verify", file=sys.stderr)
        return 1
    timeout_sec = int(os.environ.get("QA_AGENT_VERIFY_TIMEOUT_SEC") or "1200")
    poll = int(os.environ.get("QA_AGENT_VERIFY_POLL_SEC") or "10")
    url = f"{base}/v2/runs/{tid}"
    print(f"Waiting for verify on {url} (timeout={timeout_sec}s)")
    deadline = time.time() + timeout_sec
    attempt = 0
    while time.time() < deadline:
        attempt += 1
        try:
            code, body = _http_json(url, token=token or None, timeout=20)
        except Exception as e:
            print(f"attempt {attempt}: error {type(e).__name__}: {e}")
            time.sleep(poll)
            continue
        if code == 404:
            print(f"attempt {attempt}: run not found yet")
            time.sleep(poll)
            continue
        if code >= 400:
            print(f"attempt {attempt}: http={code} body={body[:500]}")
            time.sleep(poll)
            continue
        try:
            data = json.loads(body)
        except json.JSONDecodeError:
            print(f"attempt {attempt}: bad json")
            time.sleep(poll)
            continue
        steps = data.get("steps") or {}
        status = str(data.get("status") or "")
        awaiting = steps.get("awaiting_release")
        post_verify = steps.get("post_test_verify") or steps.get("verify")
        # Prefer post_test_verify (available before PDF/HITL); then awaiting_release
        releasable = None
        blockers = None
        if isinstance(post_verify, dict) and "releasable" in post_verify:
            releasable = post_verify.get("releasable")
            blockers = post_verify.get("blockers")
        elif isinstance(awaiting, dict) and "releasable" in awaiting:
            releasable = awaiting.get("releasable")
            blockers = awaiting.get("blockers")
        terminal_fail = status in {
            "failed",
            "error",
            "release_rejected",
            "hitl_timeout",
        }
        if post_verify is not None or awaiting is not None or releasable is not None or terminal_fail:
            print(f"verify reached status={status} releasable={releasable} blockers={blockers}")
            print(body[:4000])
            if terminal_fail and releasable is not True:
                print("::error::release-readiness terminal failure", file=sys.stderr)
                return 1
            if releasable is False:
                print("::error::release-readiness not releasable (SPT/UI/verify blockers)", file=sys.stderr)
                return 1
            if releasable is True:
                print("verify passed (releasable=true); HITL still pending if awaiting_release")
                return 0
            # awaiting without releasable key — treat as incomplete
            if awaiting is not None and releasable is None:
                print("::warning::awaiting_release present but releasable missing; checking matrix steps")
        # Also inspect execute_matrix / post_test_verify nested
        exec_step = steps.get("execute_matrix") or {}
        if isinstance(exec_step, dict) and exec_step.get("p0_failed"):
            # Wait until verify step exists before failing on p0 alone
            pass
        print(
            f"attempt {attempt}: status={status} steps={list(steps.keys())} "
            f"releasable={releasable}"
        )
        time.sleep(poll)
    print(f"::error::verify timeout after {timeout_sec}s for {tid}", file=sys.stderr)
    return 1


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: qa_agent_notify_ci.py detect|wait|notify|wait-verify", file=sys.stderr)
        return 2
    cmd = sys.argv[1]
    if cmd == "detect":
        return cmd_detect()
    if cmd == "wait":
        return cmd_wait()
    if cmd == "notify":
        return cmd_notify()
    if cmd in {"wait-verify", "wait_verify"}:
        return cmd_wait_verify()
    print(f"unknown command: {cmd}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
