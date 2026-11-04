#!/usr/bin/env python3
"""Publish product spt.yaml files as ConfigMaps for SPT catalog mounts.

Per-service ConfigMaps (spt-catalog-<service>) stay for tooling/traces.
Helm mounts ONE aggregate ConfigMap (spt-catalog-bundle) so qa-agent
values.yaml never lists individual services (any of N services, no trade-off).

Bundle keys: <service>.yaml → mounted at /catalog-external/<service>.yaml
(catalog_loader already accepts that layout).

The bundle includes apps-kind registrations from sibling product repos.
Agent services are skipped. catalog-external is not a source.

Usage (from am-core-services root):
  python scripts/publish-spt-catalogs.py
  python scripts/publish-spt-catalogs.py --service am-analysis
  python scripts/publish-spt-catalogs.py --namespace load-testing --namespace am-apps-dev
  python scripts/publish-spt-catalogs.py --dry-run
  python scripts/publish-spt-catalogs.py --list
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPOS = ROOT.parent
BUNDLE_NAME = "spt-catalog-bundle"

# kind: agents in am-gitops catalog/services.yaml. Do not publish these.
AGENT_SERVICES = frozenset(
    {
        "am-mcp-server",
        "am-support-agent",
        "am-mkt-agents",
        "am-ai-gateway",
        "am-asrax-ui",
        "am-db-agent",
        "am-fin-agent",
        "am-mkt-portal-ui",
        "am-qa-agents",
        "am-ui-test-agent",
        "am-tool-agent",
        "am-engage",
        "am-resume",
    }
)


def default_extra_roots() -> list[Path]:
    """Product repos that own apps-kind spt.yaml outside am-core-services/services."""
    return [
        REPOS / "am-platform",
        REPOS / "am-market",
        REPOS / "am-portfolio",
        REPOS / "am-doc-intelligence" / "services",
        REPOS / "am-trade-management",
        REPOS / "am-logging",
        REPOS / "am-gateways" / "api-gateway",
        REPOS / "am-corp-docs",
    ]


def _spt_files_under(root: Path) -> list[Path]:
    if not root.is_dir():
        return []
    found: list[Path] = []
    direct = root / "spt.yaml"
    if direct.is_file():
        found.append(direct)
    found.extend(sorted(root.glob("*/spt.yaml")))
    found.extend(sorted(root.glob("services/*/spt.yaml")))
    return found


def find_spt_yamls(service: str | None = None, extra_roots: list[Path] | None = None) -> list[Path]:
    """Apps-kind spt.yaml files. First path for a service id wins."""
    roots = [ROOT / "services", *(extra_roots if extra_roots is not None else default_extra_roots())]
    seen: set[str] = set()
    chosen: list[Path] = []
    for root in roots:
        for path in _spt_files_under(root):
            sid = service_id_from_path(path)
            if sid in AGENT_SERVICES or sid in seen:
                continue
            if service and sid != service:
                continue
            seen.add(sid)
            chosen.append(path)
    return chosen


def read_spt_text(path: Path) -> str:
    """UTF-8 text without BOM (kubectl --from-file preserves BOM otherwise)."""
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        raw = raw[3:]
    return raw.decode("utf-8")


def service_id_from_path(path: Path) -> str:
    for line in read_spt_text(path).splitlines():
        if line.startswith("service:"):
            return line.split(":", 1)[1].strip().strip("\"'")
    return path.parent.name


def kubectl_cmd(*args: str) -> list[str]:
    cmd = ["kubectl"]
    context = os.environ.get("KUBECTL_CONTEXT", "").strip()
    if context:
        cmd.extend(["--context", context])
    cmd.extend(args)
    return cmd


def kubectl_apply(namespace: str, rendered: str, *, attempts: int = 4) -> subprocess.CompletedProcess[str]:
    last: subprocess.CompletedProcess[str] | None = None
    for i in range(1, attempts + 1):
        last = subprocess.run(
            kubectl_cmd("-n", namespace, "apply", "-f", "-"),
            input=rendered,
            text=True,
            capture_output=True,
        )
        if last.returncode == 0:
            return last
        err = (last.stderr or "") + (last.stdout or "")
        retryable = any(
            s in err
            for s in (
                "client connection lost",
                "connection reset",
                "Timeout",
                "i/o timeout",
                "EOF",
                "TLS handshake timeout",
            )
        )
        print(f"kubectl apply failed (attempt {i}/{attempts})", file=sys.stderr)
        print(err, file=sys.stderr)
        if not retryable or i == attempts:
            break
        time.sleep(2**i)
    assert last is not None
    return last


def apply_configmap(path: Path, namespace: str, dry_run: bool) -> str:
    service = service_id_from_path(path)
    name = f"spt-catalog-{service}"
    with tempfile.TemporaryDirectory() as tmp:
        clean = Path(tmp) / "spt.yaml"
        clean.write_text(read_spt_text(path), encoding="utf-8", newline="\n")
        cmd = kubectl_cmd(
            "-n",
            namespace,
            "create",
            "configmap",
            name,
            f"--from-file=spt.yaml={clean}",
            "--dry-run=client",
            "-o",
            "yaml",
        )
        rendered = subprocess.check_output(cmd, text=True)
    if dry_run:
        print(rendered)
        return name
    apply = kubectl_apply(namespace, rendered)
    if apply.returncode != 0:
        apply.check_returncode()
    print(apply.stdout.strip() or f"applied {namespace}/{name}")
    return name


def apply_bundle(paths: list[Path], namespace: str, dry_run: bool) -> str:
    """One ConfigMap with every service — Helm mounts this only."""
    if not paths:
        return BUNDLE_NAME
    with tempfile.TemporaryDirectory() as tmp:
        cmd = kubectl_cmd(
            "-n",
            namespace,
            "create",
            "configmap",
            BUNDLE_NAME,
            "--dry-run=client",
            "-o",
            "yaml",
        )
        for path in paths:
            sid = service_id_from_path(path)
            clean = Path(tmp) / f"{sid}.yaml"
            clean.write_text(read_spt_text(path), encoding="utf-8", newline="\n")
            cmd.append(f"--from-file={sid}.yaml={clean}")
        rendered = subprocess.check_output(cmd, text=True)
    if dry_run:
        print(rendered)
        return BUNDLE_NAME
    apply = kubectl_apply(namespace, rendered)
    if apply.returncode != 0:
        apply.check_returncode()
    print(apply.stdout.strip() or f"applied {namespace}/{BUNDLE_NAME} ({len(paths)} services)")
    return BUNDLE_NAME


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--namespace",
        action="append",
        dest="namespaces",
        help="Target namespace (repeatable). Default: am-apps-dev",
    )
    parser.add_argument("--service", help="Single service id (per-CM only; bundle always full)")
    parser.add_argument(
        "--root",
        action="append",
        dest="roots",
        help="Extra directory to scan for spt.yaml (repeatable). Default: sibling app repos.",
    )
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--list", action="store_true", help="Print service id and path, then exit")
    parser.add_argument(
        "--skip-bundle",
        action="store_true",
        help="Only publish per-service ConfigMaps (legacy)",
    )
    args = parser.parse_args()
    extra = [Path(item) for item in args.roots] if args.roots else None
    namespaces = args.namespaces or ["am-apps-dev"]
    files = find_spt_yamls(args.service, extra)
    if not files:
        print("no matching apps-kind spt.yaml found", file=sys.stderr)
        return 2
    if args.list:
        for path in files:
            print(f"{service_id_from_path(path)}\t{path}")
        return 0
    all_files = find_spt_yamls(None, extra)
    for path in files:
        for ns in namespaces:
            print(f"publishing {path} -> {ns}/{f'spt-catalog-{service_id_from_path(path)}'}")
            apply_configmap(path, ns, args.dry_run)
    if not args.skip_bundle:
        for ns in namespaces:
            print(f"publishing bundle ({len(all_files)} services) -> {ns}/{BUNDLE_NAME}")
            apply_bundle(all_files, ns, args.dry_run)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
