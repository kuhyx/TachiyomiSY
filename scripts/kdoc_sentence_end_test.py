"""Tests for kdoc_sentence_end.py."""

from pathlib import Path

from kdoc_sentence_end import main, rewrite

SOURCE = """/**
 * Colors for the theme
 * by someone
 *
 * @property x thing
 */
class A

/** One liner without a stop */
val b = 1

/** Already fine. */
val c = 2
"""


def test_paragraph_gets_a_period() -> None:
    lines = SOURCE.splitlines(keepends=True)
    assert rewrite(lines, 2) is True
    assert lines[2] == " * by someone.\n"
    assert rewrite(lines, 2) is False


def test_inline_kdoc() -> None:
    lines = SOURCE.splitlines(keepends=True)
    assert rewrite(lines, 9) is True
    assert lines[8] == "/** One liner without a stop. */\n"
    assert rewrite(lines, 12) is False


def test_main(tmp_path: Path) -> None:
    source = tmp_path / "A.kt"
    source.write_text(SOURCE)
    log = tmp_path / "detekt.log"
    log.write_text(f"{source}:2:2: The first sentence of this KDoc does not end with the correct punctuation. [EndOfSentenceFormat]\n")
    assert main(["kdoc_sentence_end.py", str(log)]) == 0
    assert " * by someone.\n" in source.read_text()
