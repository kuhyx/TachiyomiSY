#!/usr/bin/env python3
"""Turn the KDoc of private members into line comments, from detekt's report.

Reads a detekt log for ``CommentOverPrivateFunction`` / ``CommentOverPrivateProperty``
findings (reported at the declaration's name) and rewrites the KDoc block that
precedes the declaration -- skipping annotations and modifiers between them -- as
``//`` comments with the same text, so the note survives while the rule's point
(private members carry no API documentation) is met. Anything else is printed as
MANUAL.

Usage: kdoc_to_comment.py <detekt-log>
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

FINDING = re.compile(r"^(?P<path>/[^:]+):(?P<line>\d+):\d+: .*\[CommentOverPrivate(Function|Property)\]$")
KDOC_LINE = re.compile(r"^(\s*)(/\*\*|\*/|\*)\s?(.*?)\s*$")


def kdoc_range(lines: list[str], decl_line: int) -> tuple[int, int] | None:
    """0-based [start, end] of the KDoc block above the declaration at 1-based [decl_line]."""
    index = decl_line - 2
    # Walk up over annotations, modifiers and the declaration's own leading lines.
    while index >= 0 and not lines[index].rstrip().endswith("*/"):
        stripped = lines[index].strip()
        if not stripped or stripped.startswith("@") or stripped.startswith("//"):
            index -= 1
            continue
        return None
    if index < 0:
        return None
    end = index
    while index >= 0 and "/**" not in lines[index]:
        index -= 1
    if index < 0:
        return None
    return index, end


def rewrite(lines: list[str], decl_line: int) -> bool:
    """Replace the KDoc above the declaration with `//` comments; false when there is none."""
    found = kdoc_range(lines, decl_line)
    if found is None:
        return False
    start, end = found
    indent = re.match(r"\s*", lines[start]).group(0)
    if start == end:
        text = lines[start].strip()[3:-2].strip()
        lines[start:end + 1] = [f"{indent}// {text}\n"] if text else []
        return True
    body: list[str] = []
    for raw in lines[start:end + 1]:
        match = KDOC_LINE.match(raw)
        text = match.group(3) if match else raw.strip()
        if text:
            body.append(f"{indent}// {text}\n")
    lines[start:end + 1] = body
    return True


def main(argv: list[str]) -> int:
    """CLI entry point."""
    per_file: dict[Path, set[int]] = defaultdict(set)
    for raw in Path(argv[1]).read_text().splitlines():
        match = FINDING.match(raw.strip())
        if match:
            per_file[Path(match["path"])].add(int(match["line"]))
    total = 0
    for path, decl_lines in per_file.items():
        lines = path.read_text().splitlines(keepends=True)
        # Bottom-up so an earlier rewrite never shifts a later target.
        for decl_line in sorted(decl_lines, reverse=True):
            if rewrite(lines, decl_line):
                total += 1
            else:
                print(f"MANUAL {path}:{decl_line}")
        path.write_text("".join(lines))
    print(f"rewrote {total} comments in {len(per_file)} files")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
