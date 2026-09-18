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

import socket
import sys
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import tomllib

JITPACK = "https://www.jitpack.io"
#: Repositories that also host `com.github.*` groups; an artifact served by
#: one of these is not JitPack's and is skipped.
OTHER_REPOS = ("https://repo1.maven.org/maven2", "https://plugins.gradle.org/m2")
TIMEOUT = 60.0
VENDORED = Path("gradle/vendored-m2")
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


def vendored(group: str, artifact: str, version: str) -> bool:
    """True when settings.gradle.kts serves this artifact from gradle/vendored-m2."""
    return (
        VENDORED / group.replace(".", "/") / artifact / version / f"{artifact}-{version}.pom"
    ).is_file()


def pom_url(repo: str, group: str, artifact: str, version: str) -> str:
    return f"{repo}/{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"


_addresses: dict[tuple, list] = {}
_system_getaddrinfo = socket.getaddrinfo


def cached_getaddrinfo(host, port, *hints):
    """Resolve each registry host once: ~30 HEADs against 3 hosts used to
    cost a resolver round-trip each, and the LAN router drops some of them
    (5 s per drop -- the preflight took 17 s for 11 artifacts)."""
    key = (host, port, *hints)
    if key not in _addresses:
        _addresses[key] = _system_getaddrinfo(host, port, *hints)
    return _addresses[key]


socket.getaddrinfo = cached_getaddrinfo


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


def verdict(coordinate: tuple[str, str, str]) -> tuple[str, bool]:
    """One report line for the artifact, and whether it is a failure."""
    group, artifact, version = coordinate
    if vendored(group, artifact, version):
        return f"  ok  vendored    {group}:{artifact}:{version}", False
    # A `com.github.*` group is not always JitPack: ben-manes' versions
    # plugin publishes to the plugin portal under that name.
    if any(pom_status(pom_url(r, group, artifact, version)) == 200 for r in OTHER_REPOS):
        return f"  ok  elsewhere   {group}:{artifact}:{version}", False
    status = pom_status(pom_url(JITPACK, group, artifact, version))
    mark = "ok " if status == 200 else "BAD"
    return f"  {mark} jitpack {status:3d} {group}:{artifact}:{version}", status != 200


def main(argv: list[str]) -> int:
    catalogs = [Path(a) for a in argv] or [
        Path(c) for c in DEFAULT_CATALOGS if Path(c).is_file()
    ]
    coordinates = [c for catalog in catalogs for c in jitpack_coordinates(catalog)]
    # The artifacts are independent; ask about all of them at once and
    # print in catalog order so the report is stable.
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(verdict, coordinates))
    failures = 0
    for line, failed in results:
        print(line)
        failures += failed
    if failures:
        print(
            f"JitPack preflight: {failures} artifact(s) not served; Gradle would report them as not found."
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
