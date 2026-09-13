#!/usr/bin/env python3
"""Fill in return types the explicit-API compiler asked for, where they are mechanical.

Reads a Gradle/kotlinc log for "Return type must be specified" diagnostics
and, for each, inserts ``: Type`` when the type is derivable without a
compiler: literal initialisers, ``mutableListOf<T>()``, ``override`` members
whose overridden declaration in the same source tree spells its type, and
``get() =`` overrides likewise. Anything else is printed as MANUAL.

Usage: explicit_api_types.py <gradle-log> <source-root>
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

RETURN_TYPE = "Return type must be specified in explicit API mode."
DIAGNOSTIC = re.compile(r"^e: file://(?P<path>/[^:]+):(?P<line>\d+):(?P<col>\d+) (?P<msg>.*)$")
FUN_DECL = re.compile(r"\bfun\s+(?:<[^>]+>\s+)?(?:[\w.<>?, ]+\.)?(\w+)\s*\(")
PROP_DECL = re.compile(r"\b(?:val|var)\s+(?:[\w.<>?, ]+\.)?(\w+)\s*:\s*([^=\n{,)]+?)[ \t]*(?:=|$|\bget\b|\bby\b|\{|,|\))", re.M)
LITERALS = [
    (re.compile(r'=\s*"(?:[^"\\]|\\.)*"\s*(?:\+\s*$)?$'), "String"),
    (re.compile(r"=\s*-?\d+L$"), "Long"),
    (re.compile(r"=\s*-?\d+[fF]$"), "Float"),
    (re.compile(r"=\s*-?\d+\.\d+$"), "Double"),
    (re.compile(r"=\s*-?\d{10,}$"), "Long"),
    (re.compile(r"=\s*-?\d+$"), "Int"),
    (re.compile(r"=\s*(true|false)$"), "Boolean"),
    (re.compile(r"=\s*mutableListOf<([^>]+)>\(\)$"), "MutableList<\\1>"),
    (re.compile(r"=\s*listOf<([^>]+)>\(\)$"), "List<\\1>"),
    (re.compile(r"=\s*mutableMapOf<([^>]+)>\(\)$"), "MutableMap<\\1>"),
]


def fun_return_type(signature: str) -> str | None:
    """Return type spelled after the parameter list of a one-line ``fun`` signature."""
    depth = 0
    for index, char in enumerate(signature):
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                rest = signature[index + 1 :]
                match = re.match(r"[ \t]*:[ \t]*([^={\n]+?)[ \t]*(?:=|\{|$)", rest)
                return match.group(1).strip() if match else None
    return None


def index_types(root: Path) -> dict[str, set[str]]:
    """Map every explicitly typed fun/val/var name under [root] to the types it is declared with."""
    types: dict[str, set[str]] = defaultdict(set)
    for path in root.rglob("*.kt"):
        text = path.read_text()
        for match in FUN_DECL.finditer(text):
            signature = text[match.start() : text.find("\n", text.find(")", match.end()) if text.find(")", match.end()) != -1 else match.end())]
            declared = fun_return_type(signature)
            if declared:
                types[match.group(1)].add(declared)
        for match in PROP_DECL.finditer(text):
            types[match.group(1)].add(match.group(2).strip())
    return types


def infer(line: str, types: dict[str, set[str]]) -> tuple[str, str] | None:
    """Return (name, type) for the declaration on [line] if it can be inferred."""
    stripped = line.rstrip()
    for pattern, replacement in LITERALS:
        match = pattern.search(stripped)
        if match:
            name = re.search(r"\b(?:val|var)\s+(\w+)", stripped) or FUN_DECL.search(stripped)
            return (name.group(1), match.expand(replacement)) if name else None
    if "override" not in stripped:
        return None
    name_match = FUN_DECL.search(stripped) or re.search(r"\b(?:val|var)\s+(\w+)", stripped)
    if not name_match:
        return None
    candidates = types.get(name_match.group(1), set())
    return (name_match.group(1), next(iter(candidates))) if len(candidates) == 1 else None


def insert_type(lines: list[str], line_no: int, name: str, kotlin_type: str) -> bool:
    """Insert ``: Type`` after the declaration's name or parameter list; multi-line params supported."""
    index = line_no - 1
    line = lines[index]
    fun_match = re.search(r"\bfun\b[^(]*\b" + re.escape(name) + r"\s*\(", line)
    if fun_match:
        # Find the closing paren, possibly on a later line, then insert before ` =` / ` {`.
        depth, i, j = 0, index, fun_match.end() - 1
        while i < len(lines):
            for k in range(j, len(lines[i])):
                depth += {"(": 1, ")": -1}.get(lines[i][k], 0)
                if depth == 0:
                    lines[i] = lines[i][: k + 1] + f": {kotlin_type}" + lines[i][k + 1 :]
                    return True
            i, j = i + 1, 0
        return False
    prop_match = re.search(r"\b(?:val|var)\s+" + re.escape(name) + r"\b", line)
    if not prop_match:
        return False
    lines[index] = line[: prop_match.end()] + f": {kotlin_type}" + line[prop_match.end() :]
    return True


def main(argv: list[str]) -> int:
    """CLI entry point."""
    if len(argv) != 3:
        print(__doc__, file=sys.stderr)
        return 2
    types = index_types(Path(argv[2]))
    todo: dict[Path, set[int]] = defaultdict(set)
    for raw in Path(argv[1]).read_text().splitlines():
        match = DIAGNOSTIC.match(raw.strip())
        if match and match["msg"] == RETURN_TYPE:
            todo[Path(match["path"])].add(int(match["line"]))
    fixed = 0
    for path, line_numbers in todo.items():
        lines = path.read_text().splitlines(keepends=True)
        for line_no in sorted(line_numbers, reverse=True):
            inferred = infer(lines[line_no - 1], types)
            if inferred and insert_type(lines, line_no, *inferred):
                fixed += 1
            else:
                print(f"MANUAL {path}:{line_no} {lines[line_no - 1].strip()[:100]}")
        path.write_text("".join(lines))
    print(f"inserted {fixed} return types")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
