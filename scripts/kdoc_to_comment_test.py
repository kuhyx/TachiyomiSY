"""Tests for kdoc_to_comment.py."""

from pathlib import Path

from kdoc_to_comment import main, rewrite

SOURCE = """class A {
    /**
     * Title overlay for [Item]
     *
     * @param x the thing
     */
    @Composable
    private fun overlay(x: Int) = x

    /** One-liner. */
    private val count = 1

    private val bare = 2
}
"""


def test_multiline_kdoc_becomes_line_comments() -> None:
    lines = SOURCE.splitlines(keepends=True)
    assert rewrite(lines, 8) is True
    assert "".join(lines[1:5]) == "    // Title overlay for [Item]\n    // @param x the thing\n    @Composable\n    private fun overlay(x: Int) = x\n"


def test_one_line_kdoc_and_missing_kdoc() -> None:
    lines = SOURCE.splitlines(keepends=True)
    assert rewrite(lines, 11) is True
    assert lines[9] == "    // One-liner.\n"
    assert rewrite(lines, 13) is False


def test_main_reports_manual(tmp_path: Path, capsys: object) -> None:
    source = tmp_path / "A.kt"
    source.write_text(SOURCE)
    log = tmp_path / "detekt.log"
    log.write_text(
        f"{source}:8:17: The function overlay has a comment. [CommentOverPrivateFunction]\n"
        f"{source}:13:17: The property bare has a comment. [CommentOverPrivateProperty]\n",
    )
    assert main(["kdoc_to_comment.py", str(log)]) == 0
    assert "// Title overlay for [Item]" in source.read_text()
    assert "/**" not in source.read_text().splitlines()[1]
