#!/usr/bin/env python3
"""Rewrite single-`return` function bodies as expression bodies, from detekt's report.

Reads a Gradle/detekt log for ``ExpressionBodySyntax`` findings (reported at
the ``return`` line) and turns each

    fun f(): T {
        return <expr>
    }

into ``fun f(): T = <expr>`` when the block holds nothing but that return and
the ``fun`` header (one line, or a multi-line parameter list closing with `) {`) ends
on the line before the block. Anything else is printed
as MANUAL. Run spotlessApply afterwards; the expression keeps its indentation.

Usage: expression_body.py <detekt-log>
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

MAX_LINE = 120
FINDING = re.compile(r"^(?P<path>/[^:]+):(?P<line>\d+):\d+: .*\[ExpressionBodySyntax\]$")


def rewrite(lines: list[str], return_line: int) -> bool:
    """Rewrite the function whose sole statement is the return at 1-based [return_line]."""
    if return_line < 2 or return_line > len(lines):
        return False
    ret = lines[return_line - 1]
    match = re.match(r"^(\s*)return (.*)$", ret)
    if not match:
        return False
    indent, expr_head = match.groups()
    header = lines[return_line - 2]
    # The header may span lines; its last line closes the parameter list (`) {` or `): T {`).
    is_header_end = re.match(r"^\s*\)(: .*)? \{\s*$", header) is not None and header.startswith(indent[:-4] + ")")
    is_one_line_fun = " fun " in header or header.lstrip().startswith("fun ")
    if not header.rstrip().endswith("{") or not (is_one_line_fun or is_header_end):
        return False
    outer = indent[:-4]
    end = return_line
    while end < len(lines) and lines[end] != f"{outer}}}":
        end += 1
    if end >= len(lines):
        return False
    body = lines[return_line:end]
    if any(line.strip() and not line.startswith(indent) for line in body):
        return False
    joined = header.rstrip()[:-1].rstrip() + " = " + expr_head
    if len(joined) <= MAX_LINE:
        lines[return_line - 2] = joined
        del lines[end]
        del lines[return_line - 1]
    else:
        # Too wide on one line: the expression starts on its own line, as ktlint formats it.
        lines[return_line - 2] = header.rstrip()[:-1].rstrip() + " ="
        lines[return_line - 1] = indent + expr_head
        del lines[end]
    return True


def main(argv: list[str]) -> int:
    """CLI entry point."""
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    todo: dict[Path, set[int]] = defaultdict(set)
    for raw in Path(argv[1]).read_text().splitlines():
        match = FINDING.match(raw.strip())
        if match:
            todo[Path(match["path"])].add(int(match["line"]))
    done = 0
    for path, line_numbers in todo.items():
        if not path.exists():
            print(f"MANUAL {path}: gone since the report")
            continue
        lines = path.read_text().split("\n")
        for line_no in sorted(line_numbers, reverse=True):
            if rewrite(lines, line_no):
                done += 1
            else:
                print(f"MANUAL {path}:{line_no}")
        path.write_text("\n".join(lines))
    print(f"rewrote {done} functions")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
