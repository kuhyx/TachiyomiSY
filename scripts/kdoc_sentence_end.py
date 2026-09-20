#!/usr/bin/env python3
"""End the first KDoc paragraph with a period, from detekt's ``EndOfSentenceFormat`` report.

Each finding points at the first text line of a KDoc. The paragraph runs until a
blank ``*`` line, a ``@tag`` line or ``*/``; a period is appended to its last
line unless it already ends in sentence punctuation.

Usage: kdoc_sentence_end.py <detekt-log>
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

FINDING = re.compile(r"^(?P<path>/[^:]+):(?P<line>\d+):\d+: .*\[EndOfSentenceFormat\]$")
TEXT_LINE = re.compile(r"^\s*(/\*\*|\*(?!/))\s*(?P<text>\S.*?)\s*$")
ENDS_SENTENCE = re.compile(r"[.?!:]$")


def paragraph_end(lines: list[str], first: int) -> int | None:
    """0-based index of the last text line of the paragraph starting at 0-based [first]."""
    end: int | None = None
    index = first
    while index < len(lines):
        match = TEXT_LINE.match(lines[index])
        if not match or match["text"].startswith("@") or match["text"].startswith("*/"):
            break
        end = index
        if match["text"].endswith("*/"):
            break
        index += 1
    return end


def rewrite(lines: list[str], first_line: int) -> bool:
    """Append a period to the first paragraph starting at 1-based [first_line]; false when not needed."""
    end = paragraph_end(lines, first_line - 1)
    if end is None:
        return False
    line = lines[end].rstrip("\n")
    inline_close = line.endswith("*/")
    text = line[:-2].rstrip() if inline_close else line
    if ENDS_SENTENCE.search(text):
        return False
    lines[end] = f"{text}. */\n" if inline_close else f"{text}.\n"
    return True


def main(argv: list[str]) -> int:
    """CLI entry point."""
    per_file: dict[Path, set[int]] = defaultdict(set)
    for raw in Path(argv[1]).read_text().splitlines():
        match = FINDING.match(raw.strip())
        if match:
            per_file[Path(match["path"])].add(int(match["line"]))
    total = 0
    for path, firsts in per_file.items():
        lines = path.read_text().splitlines(keepends=True)
        for first in sorted(firsts):
            if rewrite(lines, first):
                total += 1
            else:
                print(f"MANUAL {path}:{first}")
        path.write_text("".join(lines))
    print(f"ended {total} sentences in {len(per_file)} files")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
