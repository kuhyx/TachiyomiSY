"""Tests for campaign_status.py and campaign_status_worker.py."""

import fcntl
from pathlib import Path

import pytest

import campaign_status as status
import campaign_status_parts as parts
import campaign_status_worker as worker


@pytest.fixture
def state(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Path:
    """Cached results and the worker lock in a temporary directory, never the real .logs/status."""
    folder = tmp_path / "state"
    monkeypatch.setattr(parts, "STATE", folder)
    monkeypatch.setattr(worker, "STATE", folder)
    monkeypatch.setattr(worker, "LOCK", folder / "worker.lock")
    return folder


def test_failing_classes_names_failed_results(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    results = tmp_path / "app/build/test-results/testDebugUnitTest"
    results.mkdir(parents=True)
    (results / "TEST-a.b.GoodTest.xml").write_text(
        '<testsuite tests="3" failures="0" errors="0">'
    )
    (results / "TEST-a.b.BadTest.xml").write_text(
        '<testsuite tests="3" failures="1" errors="0">'
    )
    (results / "TEST-a.b.CrashTest.xml").write_text(
        '<testsuite tests="3" failures="0" errors="2">'
    )
    monkeypatch.setattr(status, "MIRROR", tmp_path)
    assert status.failing_classes() == ["BadTest", "CrashTest"]
    assert status.failing_classes(limit=1) == ["BadTest", "... and 1 more"]


def test_checklist_reads_the_finish_line(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    (tmp_path / "app").mkdir()
    (tmp_path / "app/build.gradle.kts").write_text("plugins {}\n")
    (tmp_path / "app/coverage-exceptions.txt").write_text("")
    (tmp_path / "test-wip").mkdir()
    monkeypatch.setattr(status, "REPO", tmp_path)
    lines = status.checklist_lines()
    assert "  [x] app coverage gate enforced (no `syAppCoverage` opt-in left)" in lines
    assert "  [x] app/coverage-exceptions.txt exists" in lines
    assert "  [x] TODO-app-coverage-merge.md deleted" in lines
    assert "  [ ] test-wip/ deleted" in lines


def test_coverage_without_any_report(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    monkeypatch.setattr(status, "MIRROR", tmp_path / "m")
    monkeypatch.setattr(status, "REPO", tmp_path / "r")
    assert status.coverage_lines("t", 5)[-1].startswith("no report yet")


def test_coverage_reports_totals_gaps_and_a_failed_refresh(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    report = tmp_path / "m/app/build/reports/kover/reportDebug.xml"
    report.parent.mkdir(parents=True)
    report.write_text(
        '<report><package name="p"><sourcefile name="A.kt"><counter type="LINE" missed="2" covered="1"/>'
        '</sourcefile></package><counter type="LINE" missed="2" covered="1"/></report>'
    )
    (tmp_path / "r/app").mkdir(parents=True)
    (tmp_path / "r/app/coverage-exceptions.txt").write_text("# header\na.BKt 1 why\n\n")
    monkeypatch.setattr(status, "MIRROR", tmp_path / "m")
    monkeypatch.setattr(status, "REPO", tmp_path / "r")
    parts.save("coverage", tree="t", ok=False, failed=[":app:testDebugUnitTest"])
    out = status.coverage_lines("t", 5)
    assert any("OLDER than the current tree" in line for line in out)
    assert any("last refresh FAILED: :app:testDebugUnitTest" in line for line in out)
    assert "      2 lines     0 br  p/A.kt" in out
    assert out[-1] == "branch exceptions in app/coverage-exceptions.txt: 1"


def test_refresh_starts_only_for_a_changed_tree(
    monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    spawned: list[bool] = []
    monkeypatch.setattr(status, "spawn", lambda: spawned.append(True))
    monkeypatch.setattr(status, "running", lambda: None)
    assert "STARTED" in status.refresh_line("t")
    parts.save("lint", tree="t")
    parts.save("coverage", tree="t")
    assert status.refresh_line("t") == "coverage and lint are for the current tree"
    assert spawned == [True]
    monkeypatch.setattr(status, "running", lambda: 1.0)
    assert "RUNNING" in status.refresh_line("other")


def test_running_sees_a_held_lock(state: Path) -> None:
    assert worker.running() is None
    parts.save("worker", pid=1)
    with worker.LOCK.open("a+") as held:
        fcntl.flock(held, fcntl.LOCK_EX)
        assert worker.running() is not None
    assert worker.running() is None


def test_failed_tasks_parses_the_gradle_log(state: Path) -> None:
    assert worker.failed_tasks("lint") == []
    state.mkdir(parents=True)
    (state / "lint.log").write_text(
        "> Task :app:detekt FAILED\n> Task :data:spotlessCheck\n> Task :a:b FAILED\n"
    )
    assert worker.failed_tasks("lint") == [":app:detekt", ":a:b"]


def test_worker_stops_once_both_results_match_the_tree(
    monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    trees = iter(["t1", "t2", "t2"])
    refreshed: list[str] = []

    def refresh(tree: str) -> None:
        refreshed.append(tree)
        parts.save("lint", tree=tree)
        parts.save("coverage", tree=tree)

    monkeypatch.setattr(worker, "fingerprint", lambda: next(trees))
    monkeypatch.setattr(worker, "refresh_once", refresh)
    worker.main()
    # The tree changed during the first pass, so the worker went round once more, then stopped.
    assert refreshed == ["t1", "t2"]


def test_worker_exits_when_another_holds_the_lock(
    monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    monkeypatch.setattr(
        worker, "refresh_once", lambda tree: pytest.fail("must not refresh")
    )
    state.mkdir(parents=True)
    with worker.LOCK.open("a+") as held:
        fcntl.flock(held, fcntl.LOCK_EX)
        worker.main()
