"""Helpers for campaign_status.py: Kover totals, tree fingerprints, cached results, capped Gradle."""

from __future__ import annotations

import hashlib
import json
import os
import subprocess
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
STATE = REPO / ".logs/status"
# Gradle runs only in this separate worktree, synced from REPO, so a refresh never touches the build
# dir, test results or Gradle locks of the tree being worked on.
MIRROR = REPO.parent / f"{REPO.name}-status"
# git may refresh the index (a write) on diff/status; this makes every call strictly read-only.
READ_ONLY_ENV = {**os.environ, "GIT_OPTIONAL_LOCKS": "0"}
# A hard memory ceiling for background Gradle, so a refresh can never OOM the desktop.
GRADLE_MEMORY_MAX = "8G"


@dataclass(frozen=True)
class Coverage:
    """Covered and total counts of one report (or a recorded milestone)."""

    lines: int
    lines_total: int
    branches: int
    branches_total: int

    def __str__(self) -> str:
        def pct(done: int, total: int) -> str:
            return f"{100 * done / total:6.2f}%" if total else "   n/a"

        return (
            f"lines {pct(self.lines, self.lines_total)} ({self.lines:,}/{self.lines_total:,})  "
            f"branches {pct(self.branches, self.branches_total)} ({self.branches:,}/{self.branches_total:,})"
        )


def kover_totals(report: Path) -> tuple[Coverage, list[tuple[str, int, int]]]:
    """Report-level totals plus (file, missed lines, missed branches) for every file missing something."""
    root = ET.parse(report).getroot()
    counters = {c.get("type"): c for c in root.findall("counter")}

    def pair(kind: str) -> tuple[int, int]:
        counter = counters.get(kind)
        if counter is None:
            return 0, 0
        covered, missed = int(counter.get("covered", 0)), int(counter.get("missed", 0))
        return covered, covered + missed

    lines, lines_total = pair("LINE")
    branches, branches_total = pair("BRANCH")
    files = []
    for package in root.iter("package"):
        for source in package.findall("sourcefile"):
            missed = {
                c.get("type"): int(c.get("missed", 0))
                for c in source.findall("counter")
            }
            if missed.get("LINE", 0) or missed.get("BRANCH", 0):
                name = f"{package.get('name')}/{source.get('name')}"
                files.append((name, missed.get("LINE", 0), missed.get("BRANCH", 0)))
    return Coverage(lines, lines_total, branches, branches_total), files


def run(*cmd: str, cwd: Path | None = None) -> str:
    """stdout of a command, or "" if it is missing or fails (the report degrades, it does not crash)."""
    try:
        return subprocess.run(
            cmd, capture_output=True, text=True, timeout=60, cwd=cwd, check=False
        ).stdout
    except (OSError, subprocess.TimeoutExpired):
        return ""


def fingerprint() -> str:
    """Identity of the working tree: HEAD, the tracked diff and every untracked file's size and mtime."""
    digest = hashlib.sha1()
    digest.update(run("git", "-C", str(REPO), "rev-parse", "HEAD").encode())
    diff = subprocess.run(
        ["git", "-C", str(REPO), "diff", "HEAD"], capture_output=True, check=False
    ).stdout
    digest.update(diff)
    for name in sorted(
        run(
            "git", "-C", str(REPO), "ls-files", "--others", "--exclude-standard"
        ).splitlines()
    ):
        try:
            stat = (REPO / name).stat()
        except OSError:
            continue
        digest.update(f"{name}:{stat.st_size}:{stat.st_mtime_ns}".encode())
    return digest.hexdigest()


def load(name: str) -> dict:
    """A cached result written by [save], or {} when there is none yet."""
    try:
        return json.loads((STATE / f"{name}.json").read_text())
    except (OSError, ValueError):
        return {}


def save(name: str, **values: object) -> None:
    STATE.mkdir(parents=True, exist_ok=True)
    tmp = STATE / f"{name}.json.tmp"
    tmp.write_text(json.dumps({**values, "at": time.time()}))
    tmp.replace(STATE / f"{name}.json")


def ago(stamp: float | None) -> str:
    if not stamp:
        return "never"
    minutes = (time.time() - stamp) / 60
    return f"{minutes:.0f} min ago" if minutes < 120 else f"{minutes / 60:.1f} h ago"


def gradle(log_name: str, *tasks: str) -> bool:
    """Runs Gradle niced inside a memory-capped systemd scope; output goes to .logs/status/<log_name>.log."""
    STATE.mkdir(parents=True, exist_ok=True)
    cmd = [
        "systemd-run",
        "--user",
        "--scope",
        "--quiet",
        "-p",
        f"MemoryMax={GRADLE_MEMORY_MAX}",
        "-p",
        "MemorySwapMax=512M",
        "--",
        "nice",
        "-n",
        "19",
        "ionice",
        "-c",
        "3",
        str(MIRROR / "gradlew"),
        "-p",
        str(MIRROR),
        *tasks,
        "-Dorg.gradle.jvmargs=-Xmx2g -XX:+UseSerialGC -Dfile.encoding=UTF-8 -Dsy.tree=status",
        "-Pkotlin.daemon.jvmargs=-Xmx2g",
        "-Pkotlin.compiler.execution.strategy=in-process",
    ]
    env = {
        **os.environ,
        "JAVA_HOME": os.environ.get("JAVA_HOME", "/usr/lib/jvm/java-17-openjdk"),
    }
    # The whole point of the mirror: Gradle must never run in the tree being worked on.
    if str(REPO) in cmd:
        raise RuntimeError(
            "refusing to run Gradle in the working tree; it belongs in the mirror"
        )
    with (STATE / f"{log_name}.log").open("w") as out:
        return (
            subprocess.run(
                cmd, stdout=out, stderr=subprocess.STDOUT, env=env, check=False
            ).returncode
            == 0
        )
