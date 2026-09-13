"""Tests for explicit_api_types.py."""

from pathlib import Path

from explicit_api_types import fun_return_type, index_types, infer, insert_type


def test_fun_return_type_reads_past_nested_parens() -> None:
    assert fun_return_type("fun f(a: (Int) -> Unit): List<String> =") == "List<String>"
    assert fun_return_type("fun f(a: Int) {") is None


def test_index_and_infer_overrides(tmp_path: Path) -> None:
    (tmp_path / "Base.kt").write_text("abstract class B {\n    abstract val id: Long\n    open fun name(): String = \"\"\n}\n")
    types = index_types(tmp_path)
    assert types["id"] == {"Long"} and types["name"] == {"String"}
    assert infer("    override val id get() = 1L", types) == ("id", "Long")
    assert infer("    override fun name() = \"x\"", types) == ("name", "String")
    assert infer("    fun other() = compute()", types) is None


def test_infer_literals() -> None:
    assert infer('    const val NAME = "n"', {}) == ("NAME", "String")
    assert infer("    val big = 2221515250486218861", {}) == ("big", "Long")
    assert infer("    val tags = mutableListOf<Tag>()", {}) == ("tags", "MutableList<Tag>")


def test_insert_type_handles_multiline_params() -> None:
    lines = ["fun copy(\n", "    a: Int,\n", ") = 1\n"]
    assert insert_type(lines, 1, "copy", "Int")
    assert lines[2] == "): Int = 1\n"
    lines = ["    override val id get() = 1L\n"]
    assert insert_type(lines, 1, "id", "Long")
    assert lines[0] == "    override val id: Long get() = 1L\n"
    assert not insert_type(["nothing here\n"], 1, "x", "Int")
