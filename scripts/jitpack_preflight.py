#!/usr/bin/env python3
"""Check that every JitPack artifact the catalogs pin is actually served.

JitPack builds on demand and evicts builds it has not served for a while;
Gradle then reports the artifact as "Could not find", which reads like a
typo in the catalog. This asks JitPack for each POM first and prints the HTTP
status per artifact, so a red build says "JitPack has no build for
flexible-adapter c8013533" instead of sending someone to grep the catalog.

Usage: scripts/jitpack_preflight.py [gradle/*.versions.toml ...]
Exit 0 when every POM answers 200, 1 otherwise.
"""

from __future__ import annotations

import sys
import urllib.error
import urllib.request
from pathlib import Path

import tomllib

JITPACK = "https://www.jitpack.io"
#: Repositories that also host `com.github.*` groups; an artifact served by
#: one of these is not JitPack's and is skipped.
OTHER_REPOS = ("https://repo1.maven.org/maven2", "https://plugins.gradle.org/m2")
TIMEOUT = 60.0
DEFAULT_CATALOGS = (
    "gradle/libs.versions.toml",
    "gradle/mihon.versions.toml",
    "gradle/sy.versions.toml",
)


def jitpack_coordinates(catalog: Path) -> list[tuple[str, str, str]]:
    """`(group, artifact, version)` for every `com.github.*` library."""
    data = tomllib.loads(catalog.read_text(encoding="utf-8"))
    versions = data.get("versions") or {}
    found: list[tuple[str, str, str]] = []
    for entry in (data.get("libraries") or {}).values():
        if isinstance(entry, str):
            parts = entry.split(":")
            module, version = ":".join(parts[:2]), parts[2] if len(parts) > 2 else ""
        elif isinstance(entry, dict):
            module = str(
                entry.get("module")
                or f"{entry.get('group', '')}:{entry.get('name', '')}"
            )
            raw = entry.get("version")
            version = (
                str(versions.get(raw["ref"], ""))
                if isinstance(raw, dict)
                else str(raw or "")
            )
        else:
            continue
        group, _, artifact = module.partition(":")
        if group.startswith("com.github.") and artifact and version:
            found.append((group, artifact, version))
    return found


def pom_url(repo: str, group: str, artifact: str, version: str) -> str:
    return f"{repo}/{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"


def pom_status(url: str) -> int:
    request = urllib.request.Request(
        url, method="HEAD", headers={"User-Agent": "tachiyomisy-ci-preflight"}
    )
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
            return int(response.status)
    except urllib.error.HTTPError as exc:
        return int(exc.code)
    except (urllib.error.URLError, TimeoutError, OSError):
        return 0


def main(argv: list[str]) -> int:
    catalogs = [Path(a) for a in argv] or [
        Path(c) for c in DEFAULT_CATALOGS if Path(c).is_file()
    ]
    failures = 0
    for catalog in catalogs:
        for group, artifact, version in jitpack_coordinates(catalog):
            # A `com.github.*` group is not always JitPack: ben-manes'
            # versions plugin publishes to the plugin portal under that name.
            if any(pom_status(pom_url(r, group, artifact, version)) == 200 for r in OTHER_REPOS):
                print(f"  ok  elsewhere   {group}:{artifact}:{version}")
                continue
            status = pom_status(pom_url(JITPACK, group, artifact, version))
            mark = "ok " if status == 200 else "BAD"
            print(f"  {mark} jitpack {status:3d} {group}:{artifact}:{version}")
            failures += status != 200
    if failures:
        print(
            f"JitPack preflight: {failures} artifact(s) not served; Gradle would report them as not found."
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
