#!/usr/bin/env python3
"""Copy am-specs services/*/tools.json into contract-packs/ for image bake.

Usage (from am-mcp-server dir or any cwd):
  python scripts/sync_contract_packs.py
  python scripts/sync_contract_packs.py --specs-root F:/am-repos/am-repos/am-specs
"""
from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SPECS = ROOT.parents[2] / "am-specs"  # am-repos/am-specs when layout is am-repos/*


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument(
        "--specs-root",
        type=Path,
        default=Path(__file__).resolve().parents[4] / "am-specs"
        if (Path(__file__).resolve().parents[4] / "am-specs").is_dir()
        else DEFAULT_SPECS,
    )
    ap.add_argument(
        "--out",
        type=Path,
        default=ROOT / "contract-packs" / "services",
    )
    args = ap.parse_args()
    src_services = args.specs_root / "services"
    if not src_services.is_dir():
        # workspace sibling
        alt = Path(r"f:/am-repos/am-repos/am-specs/services")
        if alt.is_dir():
            src_services = alt
        else:
            print(f"am-specs services not found: {src_services}", file=sys.stderr)
            return 1

    args.out.mkdir(parents=True, exist_ok=True)
    copied = 0
    for tools in sorted(src_services.glob("*/tools.json")):
        sid = tools.parent.name
        dest_dir = args.out / sid
        dest_dir.mkdir(parents=True, exist_ok=True)
        dest = dest_dir / "tools.json"
        shutil.copy2(tools, dest)
        copied += 1
        print(f"  {sid}: {tools.stat().st_size} bytes")
    print(f"synced {copied} packs -> {args.out}")
    return 0 if copied else 1


if __name__ == "__main__":
    raise SystemExit(main())
