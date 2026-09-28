"""The background refresher behind campaign_status.py: lint, then the app coverage report.

The report never waits for Gradle. When a cached result was made on a different tree than the current
one, it calls [spawn]; the worker takes a lock (one refresh at a time), runs lint and coverage for the
tree as it is now, records the tree fingerprint with each result, and goes round again if the tree
changed meanwhile.
"""

from __future__ import annotations

import fcntl
import os
import re
import subprocess
import sys
import time
from pathlib import Path

from campaign_status_parts import (
    MIRROR,
    READ_ONLY_ENV,
    REPO,
    STATE,
    fingerprint,
    gradle,
    load,
    save,
)

LOCK = STATE / "worker.lock"
MAX_PASSES = 3
FAILED_TASK = re.compile(r"^> Task (\S+) FAILED", re.MULTILINE)


def running() -> float | None:
    """Start time of the live worker, or None when no refresh is running."""
    try:
        with LOCK.open("a+") as handle:
            fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
            fcntl.flock(handle, fcntl.LOCK_UN)
            return None
    except BlockingIOError:
        return load("worker").get("at")
    except OSError:
        return None


def spawn() -> None:
    """Starts the worker detached from this terminal; a second one exits at once on the lock."""
    STATE.mkdir(parents=True, exist_ok=True)
    with (STATE / "worker.log").open("a") as log:
        subprocess.Popen(
            [sys.executable, str(Path(__file__).resolve())],
            stdout=log,
            stderr=subprocess.STDOUT,
            stdin=subprocess.DEVNULL,
            start_new_session=True,
            cwd=REPO,
        )


def failed_tasks(log_name: str) -> list[str]:
    try:
        return FAILED_TASK.findall(
            (STATE / f"{log_name}.log").read_text(errors="replace")
        )
    except OSError:
        return []


def sync_mirror() -> None:
    """Makes MIRROR a copy of REPO's working tree (HEAD + uncommitted + untracked); REPO is only read."""
    if not (MIRROR / ".git").exists():
        subprocess.run(["git", "-C", str(REPO), "worktree", "add", "--detach", str(MIRROR), "HEAD"], check=True)
    head = subprocess.run(
        ["git", "-C", str(REPO), "rev-parse", "HEAD"], capture_output=True, text=True, check=True, env=READ_ONLY_ENV
    ).stdout.strip()
    subprocess.run(["git", "-C", str(MIRROR), "checkout", "-q", "--detach", "--force", head], check=True)
    excludes = [f"--exclude={p}" for p in (".git", "build/", ".gradle/", ".kotlin/", ".logs/", "local.properties")]
    subprocess.run(["rsync", "-a", "--delete", *excludes, f"{REPO}/", f"{MIRROR}/"], check=True)
    if (REPO / "local.properties").exists():
        subprocess.run(["cp", str(REPO / "local.properties"), str(MIRROR / "local.properties")], check=True)


def refresh_once(tree: str) -> None:
    sync_mirror()
    started = time.time()
    ok = gradle("lint", "detekt", "spotlessCheck", "--continue")
    save("lint", tree=tree, ok=ok, failed=failed_tasks("lint"), started=started)
    started = time.time()
    ok = gradle(
        "coverage",
        ":app:testDebugUnitTest",
        ":app:koverXmlReportDebug",
        "-PsyAppCoverage",
        "--continue",
    )
    save("coverage", tree=tree, ok=ok, failed=failed_tasks("coverage"), started=started)


def main() -> None:
    STATE.mkdir(parents=True, exist_ok=True)
    with LOCK.open("a+") as handle:
        try:
            fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return
        save("worker", pid=os.getpid())
        for _ in range(MAX_PASSES):
            tree = fingerprint()
            if (
                load("lint").get("tree") == tree
                and load("coverage").get("tree") == tree
            ):
                return
            refresh_once(tree)


if __name__ == "__main__":
    main()
