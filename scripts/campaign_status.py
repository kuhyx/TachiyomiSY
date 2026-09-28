#!/usr/bin/env python3
"""Where the app coverage campaign stands, in about a second, without asking anyone.

usage: scripts/campaign_status.py [--top N]

Everything is read-only for the tree you work in. Coverage and lint come from the last background
refresh; whenever that refresh was made on a different tree than the current one, the report starts a
new one (campaign_status_worker.py: one at a time, memory-capped, in its own worktree
<repo>-status) and says so. Run the report again later to see the fresh numbers.
"""

from __future__ import annotations

import argparse
import datetime as dt
import re
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from campaign_status_parts import (
    MIRROR,
    REPO,
    Coverage,
    ago,
    fingerprint,
    kover_totals,
    load,
    run,
    save,
)
from campaign_status_worker import running, spawn

CI_CACHE_SECONDS = 120

# Recorded milestones, so progress reads as a delta rather than a bare number.
MILESTONES = [
    ("campaign start, before wave 1", Coverage(226, 87_369, 0, 21_845)),
    ("wave 1 merged (2026-09-25)", Coverage(45_151, 87_369, 5_229, 21_845)),
]


def coverage_lines(tree: str, top: int) -> list[str]:
    out = ["## app coverage (goal: 100% lines, 100% branches)"]
    cached = load("coverage")
    report = MIRROR / "app/build/reports/kover/reportDebug.xml"
    if not report.exists():
        report = REPO / "app/build/reports/kover/reportDebug.xml"
    if not report.exists():
        return [
            *out,
            "no report yet -- the background refresh is producing the first one",
        ]
    fresh = cached.get("tree") == tree and cached.get("ok")
    when = dt.datetime.fromtimestamp(report.stat().st_mtime, tz=dt.UTC).astimezone()
    out.append(
        f"report: {when:%H:%M %d.%m} ({'current tree' if fresh else 'OLDER than the current tree'})"
    )
    if cached.get("failed"):
        out.append(f"last refresh FAILED: {', '.join(cached['failed'])} -- see .logs/status/coverage.log")
        out += [f"  failing: {name}" for name in failing_classes()]
    now, files = kover_totals(report)
    out.append(f"now:      {now}")
    for label, then in MILESTONES:
        out.append(
            f"vs {label}: +{now.lines - then.lines:,} lines, +{now.branches - then.branches:,} branches"
        )
    out.append(
        f"left:     {now.lines_total - now.lines:,} lines, {now.branches_total - now.branches:,} branches"
    )
    worst = sorted(files, key=lambda f: -f[1])[:top]
    out.append(f"most missed lines (top {len(worst)}):")
    out += [f"  {ml:5d} lines {mb:5d} br  {name}" for name, ml, mb in worst]
    exceptions = REPO / "app/coverage-exceptions.txt"
    entries = exceptions.read_text().splitlines() if exceptions.exists() else []
    live = sum(1 for line in entries if line.strip() and not line.startswith("#"))
    out.append(f"branch exceptions in app/coverage-exceptions.txt: {live}")
    return out


def failing_classes(limit: int = 10) -> list[str]:
    """Test classes that failed in the mirror's last :app run, so a failed refresh says why."""
    results = MIRROR / "app/build/test-results/testDebugUnitTest"
    failed = []
    for xml in sorted(results.glob("TEST-*.xml")):
        head = xml.read_text(errors="replace")[:600]
        if re.search(r'(failures|errors)="[1-9]', head):
            failed.append(xml.stem.removeprefix("TEST-").rsplit(".", 1)[-1])
    return failed[:limit] + ([f"... and {len(failed) - limit} more"] if len(failed) > limit else [])


def module_lines() -> list[str]:
    out = ["## other modules (from their last check; i18n is generated resource code)"]
    # The mirror's reports are the fresher ones, but it only has those its refreshes built so far.
    def module_reports(root: Path) -> list[Path]:
        found = [*root.glob("*/build/reports/kover/report*.xml"), *root.glob("*/*/build/reports/kover/report*.xml")]
        return sorted(p for p in found if not str(p.relative_to(root)).startswith("app/"))

    root = next((r for r in (MIRROR, REPO) if module_reports(r)), REPO)
    reports = module_reports(root)
    for report in reports:
        out.append(
            f"  {str(report.relative_to(root)).split('/build/')[0]:28s} {kover_totals(report)[0]}"
        )
    return out if reports else [*out, "  no module reports on disk yet"]


def quality_lines(tree: str) -> list[str]:
    out = ["## file-length cap and lint"]
    cap = load("filelength")
    if cap.get("tree") != tree:
        text = run(str(REPO / "scripts/check_file_length.sh"), "--all", cwd=REPO)
        save(
            "filelength", tree=tree, ok=not text.strip(), output=text.splitlines()[:10]
        )
        cap = load("filelength")
    out.append(f"250-line cap, whole tree: {'PASS' if cap.get('ok') else 'FAIL'}")
    out += [f"  {line}" for line in cap.get("output", [])]
    lint = load("lint")
    state = (
        "current tree" if lint.get("tree") == tree else "OLDER than the current tree"
    )
    failed = ", ".join(lint.get("failed", [])) or "see .logs/status/lint.log"
    verdict = (
        ("PASS" if lint.get("ok") else f"FAIL: {failed}") if lint else "not run yet"
    )
    out.append(
        f"detekt + spotless, every module: {verdict} ({ago(lint.get('at'))}, {state})"
    )
    counts = run(
        "git", "-C", str(REPO), "grep", "-c", "@Suppress", "--", "*.kt"
    ).splitlines()
    out.append(
        f"@Suppress in Kotlin sources: {sum(int(line.rsplit(':', 1)[1]) for line in counts)}"
    )
    return out


def tests_lines() -> list[str]:
    files = list((REPO / "app/src/test").rglob("*.kt"))
    count = sum(
        len(re.findall(r"^\s*@Test\b", f.read_text(), re.MULTILINE)) for f in files
    )
    return ["## tests", f"app test files: {len(files)}, @Test methods: {count:,}"]


def git_lines() -> list[str]:
    out = [
        "## git, worktrees, CI",
        run("git", "-C", str(REPO), "status", "-sb").splitlines()[0],
    ]
    for line in run("git", "-C", str(REPO), "worktree", "list").splitlines():
        dirty = len(run("git", "-C", line.split()[0], "status", "--short").splitlines())
        out.append(f"  {line[:90]}  ({dirty} uncommitted)")
    # CI state changes over minutes, and the network call was most of the report's run time.
    ci = load("ci")
    if time.time() - ci.get("at", 0) > CI_CACHE_SECONDS:
        save("ci", text=run("gh", "run", "list", "-R", "kuhyx/TachiyomiSY", "-L", "4"))
        ci = load("ci")
    for line in ci.get("text", "").splitlines():
        cols = [*line.split("\t"), "", "", "", ""]
        out.append(f"  CI {cols[0]:10s} {cols[1]:8s} {cols[3]}: {cols[2][:60]}")
    return out


def checklist_lines() -> list[str]:
    build = (REPO / "app/build.gradle.kts").read_text()
    checks = [
        (
            "app coverage gate enforced (no `syAppCoverage` opt-in left)",
            "syAppCoverage" not in build,
        ),
        (
            "app/coverage-exceptions.txt exists",
            (REPO / "app/coverage-exceptions.txt").exists(),
        ),
        (
            "TODO-app-coverage-merge.md deleted",
            not (REPO / "TODO-app-coverage-merge.md").exists(),
        ),
        ("test-wip/ deleted", not (REPO / "test-wip").exists()),
    ]
    return [
        "## finish line (handoff step 10)",
        *(f"  [{'x' if done else ' '}] {label}" for label, done in checks),
    ]


def refresh_line(tree: str) -> str:
    since = running()
    if since:
        return f"background refresh RUNNING (started {ago(since)}); run this again later for fresh numbers"
    if load("lint").get("tree") != tree or load("coverage").get("tree") != tree:
        spawn()
        return "tree changed since the last refresh -> background refresh STARTED (~15 min, memory-capped)"
    return "coverage and lint are for the current tree"


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--top", type=int, default=15)
    top = parser.parse_args().top
    tree = fingerprint()
    with ThreadPoolExecutor() as pool:
        parts = [
            pool.submit(coverage_lines, tree, top),
            pool.submit(module_lines),
            pool.submit(quality_lines, tree),
            pool.submit(tests_lines),
            pool.submit(git_lines),
            pool.submit(checklist_lines),
        ]
        print(f"# TachiyomiSY coverage campaign -- {dt.datetime.now(dt.UTC).astimezone():%Y-%m-%d %H:%M}")
        print(refresh_line(tree))
        for part in parts:
            print("\n" + "\n".join(part.result()))


if __name__ == "__main__":
    main()
