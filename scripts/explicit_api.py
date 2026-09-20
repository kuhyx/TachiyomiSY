#!/usr/bin/env python3
"""Apply the Kotlin compiler's explicit-API "Visibility must be specified" diagnostics.

Reads a Gradle/kotlinc log, inserts ``public `` at every reported
position (the start of the declaration's modifier list) and prints the
diagnostics it cannot fix mechanically, which are the missing return
types: those need a human, since the type is exactly what the compiler
declined to infer for the public surface.

Usage: explicit_api.py [--visibility public|internal] <gradle-log>

``--visibility internal`` is for an application module: it has no public API,
so ``internal`` satisfies strict mode without the return types or KDoc that
``public`` would then demand of every declaration.
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

VISIBILITY = "Visibility must be specified in explicit API mode."
DIAGNOSTIC = re.compile(r"^e: file://(?P<path>/[^:]+):(?P<line>\d+):(?P<col>\d+) (?P<msg>.*)$")


def parse_log(text: str) -> tuple[dict[Path, set[tuple[int, int]]], list[str]]:
    """Return {file: {(line, col)}} for visibility fixes plus the other diagnostics verbatim."""
    fixes: dict[Path, set[tuple[int, int]]] = defaultdict(set)
    others: list[str] = []
    for raw in text.splitlines():
        match = DIAGNOSTIC.match(raw.strip())
        if not match:
            continue
        if match["msg"] == VISIBILITY:
            fixes[Path(match["path"])].add((int(match["line"]), int(match["col"])))
        else:
            others.append(raw.strip())
    return fixes, others


def apply_fixes(path: Path, positions: set[tuple[int, int]], visibility: str = "public") -> int:
    """Insert ``<visibility> `` at each 1-based (line, col); returns the count applied."""
    lines = path.read_text().splitlines(keepends=True)
    applied = 0
    modifier = f"{visibility} "
    # Bottom-up and right-to-left so earlier insertions never shift later targets.
    for line_no, col in sorted(positions, reverse=True):
        line = lines[line_no - 1]
        index = col - 1
        if line[index:].startswith(modifier):
            continue
        lines[line_no - 1] = line[:index] + modifier + line[index:]
        applied += 1
    path.write_text("".join(lines))
    return applied


def main(argv: list[str]) -> int:
    """CLI entry point."""
    args = argv[1:]
    visibility = "public"
    if len(args) == 3 and args[0] == "--visibility" and args[1] in {"public", "internal"}:
        visibility, args = args[1], args[2:]
    if len(args) != 1:
        print(__doc__, file=sys.stderr)
        return 2
    fixes, others = parse_log(Path(args[0]).read_text())
    total = sum(apply_fixes(path, positions, visibility) for path, positions in fixes.items())
    print(f"inserted '{visibility}' {total} times in {len(fixes)} files")
    for line in others:
        print(f"MANUAL {line}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
