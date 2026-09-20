#!/usr/bin/env python3
"""Name every `@ProtoNumber(n)` tag in the given files as a private constant.

`@ProtoNumber(14) var viewer: Int` becomes `@ProtoNumber(BACKUP_MANGA_VIEWER) var viewer: Int`
with `private const val BACKUP_MANGA_VIEWER = 14` above the first declaration -- the data
module's convention for kotlinx.serialization tags (detekt MagicNumber). Constants are
prefixed with the enclosing class in SCREAMING_SNAKE_CASE; a tag whose value is already a
constant is left alone.

Usage: proto_numbers.py <file.kt>...
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

CLASS = re.compile(r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:internal |public |private )?(?:data |sealed |open )?class (\w+)")
TAG = re.compile(r"@ProtoNumber\((\d+)\)(?P<rest>\s+(?:override\s+)?va[lr]\s+(?P<name>\w+))")


def snake(name: str) -> str:
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).upper()


def rewrite(text: str) -> tuple[str, int]:
    lines = text.splitlines(keepends=True)
    constants: list[str] = []
    current = "FIELD"
    first_decl: int | None = None
    for i, line in enumerate(lines):
        match = CLASS.match(line)
        if match:
            current = snake(match.group(1))
            if first_decl is None:
                first_decl = i
        if first_decl is None and re.match(r"^\s*(@|internal |public |private |data |sealed |class |object |enum )", line) and not line.startswith("@file"):
            first_decl = i

        def replace(m: re.Match[str]) -> str:
            name = f"{current}_{snake(m['name'])}"
            constants.append(f"private const val {name} = {m.group(1)}\n")
            return f"@ProtoNumber({name}){m['rest']}"

        lines[i] = TAG.sub(replace, line)
    if not constants:
        return text, 0
    assert first_decl is not None
    # Annotations attached to the first declaration sit right above it: insert before them.
    insert = first_decl
    while insert > 0 and lines[insert - 1].strip().startswith("@"):
        insert -= 1
    lines[insert:insert] = constants + ["\n"]
    return "".join(lines), len(constants)


def main(argv: list[str]) -> int:
    for arg in argv[1:]:
        path = Path(arg)
        text, count = rewrite(path.read_text())
        path.write_text(text)
        print(f"{path.name}: {count} tags")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
