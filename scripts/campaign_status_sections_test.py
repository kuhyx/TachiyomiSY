"""Tests for the report sections of campaign_status.py and the worker's sync and refresh."""

import subprocess
import sys
from pathlib import Path

import pytest

import campaign_status as status
import campaign_status_parts as parts
import campaign_status_worker as worker

MODULE_REPORT = '<report><counter type="LINE" missed="0" covered="4"/><counter type="BRANCH" missed="1" covered="1"/></report>'


@pytest.fixture
def state(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Path:
    """Cached results in a temporary directory, never the real .logs/status."""
    folder = tmp_path / "state"
    monkeypatch.setattr(parts, "STATE", folder)
    monkeypatch.setattr(worker, "STATE", folder)
    monkeypatch.setattr(worker, "LOCK", folder / "worker.lock")
    return folder


def test_module_lines_prefer_the_mirror_and_skip_app(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    mirror = tmp_path / "m"
    for module in ("domain", "core/common", "app"):
        report = mirror / module / "build/reports/kover/reportDebug.xml"
        report.parent.mkdir(parents=True)
        report.write_text(MODULE_REPORT)
    monkeypatch.setattr(status, "MIRROR", mirror)
    monkeypatch.setattr(status, "REPO", tmp_path / "r")
    out = status.module_lines()
    assert [line.split()[0] for line in out[1:]] == ["core/common", "domain"]
    assert "branches  50.00% (1/2)" in out[1]


def test_module_lines_without_reports(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(status, "MIRROR", tmp_path / "m")
    monkeypatch.setattr(status, "REPO", tmp_path / "r")
    assert status.module_lines()[-1] == "  no module reports on disk yet"


def test_quality_lines_cache_the_cap_per_tree(
    monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    calls: list[tuple[str, ...]] = []

    def fake_run(*cmd: str, cwd: Path | None = None) -> str:
        calls.append(cmd)
        if cmd[0].endswith("check_file_length.sh"):
            return "app/Big.kt: 300 lines\n"
        return "a.kt:2\nb.kt:3\n"

    monkeypatch.setattr(status, "run", fake_run)
    out = status.quality_lines("t")
    assert "250-line cap, whole tree: FAIL" in out and "  app/Big.kt: 300 lines" in out
    assert (
        "detekt + spotless, every module: not run yet (never, OLDER than the current tree)"
        in out
    )
    assert out[-1] == "@Suppress in Kotlin sources: 5"
    parts.save("lint", tree="t", ok=False, failed=[":app:detekt"])
    out = status.quality_lines("t")
    assert sum(1 for c in calls if c[0].endswith("check_file_length.sh")) == 1
    assert any("FAIL: :app:detekt" in line and "current tree" in line for line in out)


def test_tests_lines_count_test_methods(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    tests = tmp_path / "app/src/test/x"
    tests.mkdir(parents=True)
    (tests / "ATest.kt").write_text(
        "class A {\n    @Test\n    fun a() {}\n    @Test fun b() {}\n}\n"
    )
    (tests / "Rig.kt").write_text("// @Test in a comment is not at a line start\n")
    monkeypatch.setattr(status, "REPO", tmp_path)
    assert status.tests_lines() == ["## tests", "app test files: 2, @Test methods: 2"]


def test_git_lines_cache_ci(monkeypatch: pytest.MonkeyPatch, state: Path) -> None:
    gh_calls: list[str] = []

    def fake_run(*cmd: str, cwd: Path | None = None) -> str:
        if cmd[0] == "gh":
            gh_calls.append("gh")
            return "completed\tsuccess\tmsg\tgates\tmaster\n"
        if "worktree" in cmd:
            return "/wt/a  abc [master]\n"
        if "-sb" in cmd:
            return "## master...origin/master\n"
        return " M x\n M y\n"

    monkeypatch.setattr(status, "run", fake_run)
    out = status.git_lines()
    assert out[1] == "## master...origin/master"
    assert out[2].endswith("(2 uncommitted)")
    assert out[3].startswith("  CI completed  success  gates: msg")
    status.git_lines()
    assert gh_calls == ["gh"]


def test_main_prints_every_section(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture[str]
) -> None:
    monkeypatch.setattr(sys, "argv", ["campaign_status.py", "--top", "3"])
    monkeypatch.setattr(status, "fingerprint", lambda: "t")
    monkeypatch.setattr(status, "refresh_line", lambda tree: "refresh-line")
    for name in ("module_lines", "tests_lines", "git_lines", "checklist_lines"):
        monkeypatch.setattr(status, name, lambda name=name: [f"## {name}"])
    monkeypatch.setattr(
        status, "coverage_lines", lambda tree, top: [f"## coverage top={top}"]
    )
    monkeypatch.setattr(status, "quality_lines", lambda tree: [f"## quality {tree}"])
    status.main()
    text = capsys.readouterr().out
    assert (
        "refresh-line" in text
        and "## coverage top=3" in text
        and "## quality t" in text
    )
    assert text.index("## coverage") < text.index("## checklist_lines")


def test_spawn_detaches_the_worker(
    monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    started: list[dict] = []
    monkeypatch.setattr(
        worker.subprocess, "Popen", lambda cmd, **kw: started.append({"cmd": cmd, **kw})
    )
    worker.spawn()
    assert started[0]["cmd"][1].endswith("campaign_status_worker.py")
    assert started[0]["start_new_session"] is True
    assert (state / "worker.log").exists()


def test_sync_mirror_copies_the_tree_and_never_writes_repo(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    repo, mirror = tmp_path / "repo", tmp_path / "mirror"
    repo.mkdir()
    (repo / "local.properties").write_text("sdk.dir=/x\n")
    calls: list[list[str]] = []

    def fake_run(cmd: list[str], **kwargs: object) -> subprocess.CompletedProcess[str]:
        calls.append(cmd)
        return subprocess.CompletedProcess(cmd, 0, stdout="abc123\n")

    monkeypatch.setattr(worker, "REPO", repo)
    monkeypatch.setattr(worker, "MIRROR", mirror)
    monkeypatch.setattr(worker.subprocess, "run", fake_run)
    worker.sync_mirror()
    assert calls[0][3:6] == ["worktree", "add", "--detach"]
    assert calls[2][-1] == "abc123"
    rsync = calls[3]
    assert rsync[0] == "rsync" and rsync[-2:] == [f"{repo}/", f"{mirror}/"]
    assert "--exclude=.logs/" in rsync
    assert calls[4] == [
        "cp",
        str(repo / "local.properties"),
        str(mirror / "local.properties"),
    ]
    (mirror / ".git").mkdir(parents=True)
    (repo / "local.properties").unlink()
    calls.clear()
    worker.sync_mirror()
    assert [c[0] for c in calls] == ["git", "git", "rsync"]


def test_refresh_once_records_lint_then_coverage(
    monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    runs: list[tuple[str, ...]] = []
    monkeypatch.setattr(worker, "sync_mirror", lambda: runs.append(("sync",)))
    monkeypatch.setattr(
        worker,
        "gradle",
        lambda name, *tasks: runs.append((name, *tasks)) or name == "lint",
    )
    worker.refresh_once("t")
    assert [r[0] for r in runs] == ["sync", "lint", "coverage"]
    assert ":app:koverXmlReportDebug" in runs[2]
    assert parts.load("lint")["ok"] is True
    assert (
        parts.load("coverage")["ok"] is False and parts.load("coverage")["tree"] == "t"
    )


def test_worker_gives_up_after_max_passes(
    monkeypatch: pytest.MonkeyPatch, state: Path
) -> None:
    trees = iter(str(n) for n in range(10))
    refreshed: list[str] = []
    monkeypatch.setattr(worker, "fingerprint", lambda: next(trees))
    monkeypatch.setattr(worker, "refresh_once", refreshed.append)
    worker.main()
    assert len(refreshed) == worker.MAX_PASSES


def test_coverage_falls_back_to_the_repo_report(tmp_path: Path, monkeypatch: pytest.MonkeyPatch, state: Path) -> None:
    report = tmp_path / "r/app/build/reports/kover/reportDebug.xml"
    report.parent.mkdir(parents=True)
    report.write_text(MODULE_REPORT)
    monkeypatch.setattr(status, "MIRROR", tmp_path / "m")
    monkeypatch.setattr(status, "REPO", tmp_path / "r")
    parts.save("coverage", tree="t", ok=True)
    out = status.coverage_lines("t", 5)
    assert any("(current tree)" in line for line in out)
    assert not any("FAILED" in line for line in out)
    assert out[-1] == "branch exceptions in app/coverage-exceptions.txt: 0"


def test_fingerprint_skips_a_file_that_vanished(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(parts, "REPO", tmp_path)
    monkeypatch.setattr(parts, "run", lambda *cmd, **kw: "ghost.txt\n" if "ls-files" in cmd else "")
    assert len(parts.fingerprint()) == 40


def test_the_script_runs_standalone() -> None:
    script = Path(status.__file__)
    done = subprocess.run([sys.executable, str(script), "--help"], capture_output=True, text=True, check=False)
    assert done.returncode == 0 and "--top" in done.stdout
