"""Tests for expression_body.py."""

from pathlib import Path

import pytest

from expression_body import main, rewrite


def test_rewrite_joins_short_bodies() -> None:
    lines = ["class A {", "    fun f(): Int {", "        return 1", "    }", "}"]
    assert rewrite(lines, 3)
    assert lines == ["class A {", "    fun f(): Int = 1", "}"]


def test_rewrite_keeps_multiline_expression_and_wraps_wide_headers() -> None:
    header = "    public fun aVeryLongFunctionName(argumentOne: String, argumentTwo: String, argumentThree: String): String {"
    lines = [header, "        return argumentOne +", "            argumentTwo + argumentOne + argumentTwo", "    }"]
    assert rewrite(lines, 2)
    assert lines == [
        header[:-1].rstrip() + " =",
        "        argumentOne +",
        "            argumentTwo + argumentOne + argumentTwo",
    ]


def test_rewrite_refuses_other_shapes() -> None:
    assert not rewrite(["fun f() {", "    val x = 1", "    return x", "}"], 3)
    assert not rewrite(["    return 1"], 1)
    assert not rewrite(["fun f() {", "    return 1", "// no closing brace at the outer indent"], 2)


def test_main_applies_findings_and_reports_manual(tmp_path: Path, capsys: pytest.CaptureFixture[str]) -> None:
    source = tmp_path / "A.kt"
    source.write_text("fun f(): Int {\n    return 1\n}\n\nfun g(): Int {\n    val x = 2\n    return x\n}\n")
    gone = tmp_path / "Gone.kt"
    log = tmp_path / "detekt.log"
    log.write_text(
        f"{source}:2:5: Functions with exact one statement [ExpressionBodySyntax]\n"
        f"{source}:7:5: Functions with exact one statement [ExpressionBodySyntax]\n"
        f"{gone}:1:1: Functions with exact one statement [ExpressionBodySyntax]\n",
    )
    assert main(["expression_body.py", str(log)]) == 0
    assert source.read_text() == "fun f(): Int = 1\n\nfun g(): Int {\n    val x = 2\n    return x\n}\n"
    out = capsys.readouterr().out
    assert f"MANUAL {source}:7" in out
    assert "gone since the report" in out
    assert "rewrote 1 functions" in out


def test_main_usage() -> None:
    assert main(["expression_body.py"]) == 2


def test_multiline_header(tmp_path: Path) -> None:
    source = tmp_path / "A.kt"
    source.write_text("fun f(\n    a: Int,\n): Int {\n    return a + 1\n}\n")
    log = tmp_path / "detekt.log"
    log.write_text(f"{source}:4:5: Use expression body. [ExpressionBodySyntax]\n")
    assert main(["expression_body.py", str(log)]) == 0
    assert source.read_text() == "fun f(\n    a: Int,\n): Int = a + 1\n"
