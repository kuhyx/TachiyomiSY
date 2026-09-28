"""Tests for campaign_status_parts.py."""

import time
from pathlib import Path

import pytest

import campaign_status_parts as parts
from campaign_status_parts import Coverage, ago, kover_totals

REPORT = """<?xml version="1.0" encoding="UTF-8"?>
<report name="app">
  <package name="eu/kanade/a">
    <sourcefile name="Done.kt">
      <counter type="LINE" missed="0" covered="10"/>
      <counter type="BRANCH" missed="0" covered="4"/>
    </sourcefile>
    <sourcefile name="Gap.kt">
      <counter type="LINE" missed="2" covered="8"/>
      <counter type="BRANCH" missed="3" covered="1"/>
    </sourcefile>
  </package>
  <package name="exh">
    <sourcefile name="BranchOnly.kt">
      <counter type="LINE" missed="0" covered="5"/>
      <counter type="BRANCH" missed="1" covered="1"/>
    </sourcefile>
  </package>
  <counter type="LINE" missed="2" covered="23"/>
  <counter type="BRANCH" missed="4" covered="6"/>
</report>
"""


def test_coverage_prints_percentages_and_counts() -> None:
    text = str(Coverage(1_234, 2_000, 1, 4))
    assert text == "lines  61.70% (1,234/2,000)  branches  25.00% (1/4)"


def test_coverage_without_branches_says_na() -> None:
    assert "branches    n/a (0/0)" in str(Coverage(1, 1, 0, 0))


def test_kover_totals_reads_report_and_every_file_missing_something(
    tmp_path: Path,
) -> None:
    report = tmp_path / "r.xml"
    report.write_text(REPORT)
    total, files = kover_totals(report)
    assert total == Coverage(23, 25, 6, 10)
    assert files == [("eu/kanade/a/Gap.kt", 2, 3), ("exh/BranchOnly.kt", 0, 1)]


def test_kover_totals_without_branch_counter(tmp_path: Path) -> None:
    report = tmp_path / "r.xml"
    report.write_text('<report><counter type="LINE" missed="0" covered="3"/></report>')
    assert kover_totals(report) == (Coverage(3, 3, 0, 0), [])


def test_run_degrades_to_empty_output() -> None:
    assert parts.run("echo", "hi") == "hi\n"
    assert parts.run("/nonexistent/binary") == ""


def test_save_then_load_round_trips(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(parts, "STATE", tmp_path / "state")
    assert parts.load("x") == {}
    parts.save("x", tree="abc", ok=True)
    loaded = parts.load("x")
    assert loaded["tree"] == "abc" and loaded["ok"] is True
    assert abs(loaded["at"] - time.time()) < 60
    assert not list((tmp_path / "state").glob("*.tmp"))


def test_load_ignores_a_corrupt_cache(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(parts, "STATE", tmp_path)
    (tmp_path / "x.json").write_text("{not json")
    assert parts.load("x") == {}


def test_ago() -> None:
    assert ago(None) == "never"
    assert ago(time.time() - 300) == "5 min ago"
    assert ago(time.time() - 3 * 3600) == "3.0 h ago"


def test_fingerprint_follows_untracked_files(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    repo = tmp_path / "repo"
    repo.mkdir()
    for cmd in (["init", "-q"], ["commit", "-q", "--allow-empty", "-m", "x"]):
        parts.subprocess.run(
            ["git", "-C", str(repo), "-c", "user.name=t", "-c", "user.email=t@t", *cmd],
            check=True,
        )
    monkeypatch.setattr(parts, "REPO", repo)
    before = parts.fingerprint()
    assert parts.fingerprint() == before
    (repo / "new.txt").write_text("x")
    assert parts.fingerprint() != before


def test_gradle_refuses_the_working_tree(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(parts, "STATE", tmp_path)
    monkeypatch.setattr(parts, "MIRROR", parts.REPO)
    with pytest.raises(RuntimeError, match="working tree"):
        parts.gradle("lint", "detekt")


def test_gradle_runs_in_the_mirror_and_logs(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    seen: list[list[str]] = []

    class Done:
        returncode = 0

    def fake_run(cmd: list[str], **kwargs: object) -> Done:
        seen.append(cmd)
        return Done()

    monkeypatch.setattr(parts, "STATE", tmp_path)
    monkeypatch.setattr(parts, "MIRROR", tmp_path / "mirror")
    monkeypatch.setattr(parts.subprocess, "run", fake_run)
    assert parts.gradle("lint", "detekt") is True
    assert seen[0][:2] == ["systemd-run", "--user"]
    assert f"MemoryMax={parts.GRADLE_MEMORY_MAX}" in seen[0]
    assert str(tmp_path / "mirror" / "gradlew") in seen[0]
    assert (tmp_path / "lint.log").exists()
