#!/usr/bin/env python3
"""Publish services/*/spt.yaml as ConfigMaps into the load-testing namespace.

Usage (from am-core-services root):
  python scripts/publish-spt-catalogs.py
  python scripts/publish-spt-catalogs.py --namespace load-testing --dry-run
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def find_spt_yamls() -> list[Path]:
    return sorted((ROOT / "services").glob("*/spt.yaml"))


def apply_configmap(path: Path, namespace: str, dry_run: bool) -> None:
    data = path.read_text(encoding="utf-8")
    service = None
    for line in data.splitlines():
        if line.startswith("service:"):
            service = line.split(":", 1)[1].strip().strip("\"'")
            break
    if not service:
        service = path.parent.name
    name = f"spt-catalog-{service}"
    cmd = [
        "kubectl",
        "-n",
        namespace,
        "create",
        "configmap",
        name,
        f"--from-file=spt.yaml={path}",
        "--dry-run=client",
        "-o",
        "yaml",
    ]
    rendered = subprocess.check_output(cmd, text=True)
    if dry_run:
        print(rendered)
        return
    apply = subprocess.run(
        ["kubectl", "-n", namespace, "apply", "-f", "-"],
        input=rendered,
        text=True,
        check=True,
        capture_output=True,
    )
    print(apply.stdout.strip() or f"applied {name}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--namespace", default="load-testing")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    files = find_spt_yamls()
    if not files:
        print("no services/*/spt.yaml found", file=sys.stderr)
        return 2
    for path in files:
        print(f"publishing {path}")
        apply_configmap(path, args.namespace, args.dry_run)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
