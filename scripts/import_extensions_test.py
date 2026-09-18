"""Tests for import_extensions.py."""

from pathlib import Path

import pytest

from import_extensions import add_import, main


def test_add_import_after_last_import_once(tmp_path: Path) -> None:
    source = tmp_path / "A.kt"
    source.write_text("package x\n\nimport a.B\n\nclass A\n")
    assert add_import(source, "import tachiyomi.domain.manga.model.sorting")
    assert not add_import(source, "import tachiyomi.domain.manga.model.sorting")
    assert source.read_text() == "package x\n\nimport a.B\nimport tachiyomi.domain.manga.model.sorting\n\nclass A\n"


def test_add_import_after_package_when_no_imports(tmp_path: Path) -> None:
    source = tmp_path / "A.kt"
    source.write_text("package x\n\nclass A\n")
    assert add_import(source, "import y.z")
    assert source.read_text() == "package x\nimport y.z\n\nclass A\n"


def test_main_adds_known_receivers_and_reports_unknown(tmp_path: Path, capsys: pytest.CaptureFixture[str]) -> None:
    source = tmp_path / "A.kt"
    source.write_text("package x\n\nimport a.B\n")
    log = tmp_path / "build.log"
    log.write_text(
        f"e: file://{source}:3:1 Unresolved reference 'sorting' on receiver of type 'Manga'.\n"
        f"e: file://{source}:4:1 Unresolved reference 'contains' on receiver of type 'Pins?'.\n"
        f"e: file://{source}:5:1 Unresolved reference 'foo' on receiver of type 'Other'.\n",
    )
    assert main(["import_extensions.py", str(log)]) == 0
    assert source.read_text() == (
        "package x\n\nimport a.B\nimport tachiyomi.domain.manga.model.sorting\n"
        "import tachiyomi.domain.source.model.contains\n"
    )
    out = capsys.readouterr().out
    assert "MANUAL" in out and "Other" in out
    assert "added 2 imports in 1 files" in out


def test_main_usage() -> None:
    assert main(["import_extensions.py"]) == 2
