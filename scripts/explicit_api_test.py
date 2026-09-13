"""Tests for explicit_api.py."""

from pathlib import Path

from explicit_api import apply_fixes, parse_log

LOG = """
> Task :compileKotlin
e: file:///tmp/x/A.kt:3:1 Visibility must be specified in explicit API mode.
e: file:///tmp/x/A.kt:3:1 Visibility must be specified in explicit API mode.
e: file:///tmp/x/A.kt:5:5 Visibility must be specified in explicit API mode.
e: file:///tmp/x/B.kt:2:1 Return type must be specified in explicit API mode.
w: file:///tmp/x/A.kt:1:1 Some warning
"""


def test_parse_log_splits_fixable_from_manual() -> None:
    fixes, others = parse_log(LOG)
    assert fixes == {Path("/tmp/x/A.kt"): {(3, 1), (5, 5)}}
    assert others == ["e: file:///tmp/x/B.kt:2:1 Return type must be specified in explicit API mode."]


def test_apply_fixes_inserts_public_once(tmp_path: Path) -> None:
    source = tmp_path / "A.kt"
    source.write_text("package x\n\nabstract class A {\n    val a = 1\n    open fun f() = 2\n    fun g(): Int = 3\n}\n")
    assert apply_fixes(source, {(3, 1), (5, 5), (6, 5)}) == 3
    assert source.read_text() == (
        "package x\n\npublic abstract class A {\n    val a = 1\n    public open fun f() = 2\n"
        "    public fun g(): Int = 3\n}\n"
    )
    assert apply_fixes(source, {(3, 1)}) == 0
