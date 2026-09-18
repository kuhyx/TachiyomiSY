#!/usr/bin/env python3
"""Add imports for members that moved out of a class into extensions.

Reads a Gradle/kotlinc log for ``Unresolved reference 'x' on receiver of type
'T'`` diagnostics and, for every receiver type listed in EXTENSIONS, inserts
``import <package>.x`` into the file. Anything else is printed as MANUAL.

Usage: import_extensions.py <gradle-log>
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

#: Receiver type -> package that now holds its former members as extensions.
EXTENSIONS = {
    "Manga": "tachiyomi.domain.manga.model",
    "Chapter": "tachiyomi.domain.chapter.model",
    "Pins": "tachiyomi.domain.source.model",
    "Release": "tachiyomi.domain.release.model",
    "LibrarySort": "tachiyomi.domain.library.model",
    "FavoriteEntry": "tachiyomi.domain.manga.model",
}
DIAGNOSTIC = re.compile(
    r"^e: file://(?P<path>/[^:]+):\d+:\d+ Unresolved reference '(?P<name>\w+)' on receiver of type '(?P<type>\w+)\??'\.$",
)


def add_import(path: Path, import_line: str) -> bool:
    """Insert [import_line] after the last import (or the package line); false if present."""
    lines = path.read_text().split("\n")
    if import_line in lines:
        return False
    anchors = [i for i, line in enumerate(lines) if line.startswith("import ")]
    at = anchors[-1] + 1 if anchors else next(i for i, line in enumerate(lines) if line.startswith("package ")) + 1
    lines.insert(at, import_line)
    path.write_text("\n".join(lines))
    return True


def main(argv: list[str]) -> int:
    """CLI entry point."""
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    wanted: dict[Path, set[str]] = defaultdict(set)
    for raw in Path(argv[1]).read_text().splitlines():
        match = DIAGNOSTIC.match(raw.strip())
        if not match:
            continue
        package = EXTENSIONS.get(match["type"])
        if package is None:
            print(f"MANUAL {raw.strip()}")
            continue
        wanted[Path(match["path"])].add(f"import {package}.{match['name']}")
    added = sum(add_import(path, line) for path, lines in wanted.items() for line in sorted(lines))
    print(f"added {added} imports in {len(wanted)} files")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
