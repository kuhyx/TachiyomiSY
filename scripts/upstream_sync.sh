#!/bin/bash

# ============================================================================
# Keep this fork on top of upstream: fetch, mirror, rebase, gate, push.
#
#   1. fetch jobobby04/TachiyomiSY master
#   2. push it verbatim to the fork's `upstream` branch (a pristine mirror,
#      so "what did we change?" is always `git diff upstream master`)
#   3. if master already sits on upstream's tip: status=current, exit 0
#   4. rebase master onto it; a conflict lists the files, aborts, exit 3
#   5. run scripts/ci_gates.sh; red is exit 4 -- nothing is pushed
#   6. force-push (with lease) the rebased master: status=pushed, exit 0
#
# Run daily by .github/workflows/upstream-sync.yml, and by hand:
#   scripts/upstream_sync.sh --dry-run     # everything except the pushes
#
# Env:
#   SYNC_OUTPUT     file that receives key=value lines (status=, conflicts=)
#                   -- the workflow passes $GITHUB_OUTPUT
#   SKIP_GATES=1    rebase and push without running the gates (never in CI)
# ============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT
readonly UPSTREAM_URL="https://github.com/jobobby04/TachiyomiSY.git"
readonly UPSTREAM_REMOTE="upstream"
readonly UPSTREAM_BRANCH="master"
readonly ORIGIN_REMOTE="origin"
readonly BRANCH="master"
readonly MIRROR_BRANCH="upstream"
DRY_RUN=0

emit() {
    # key=value for the caller; also visible in the log.
    echo "$1"
    if [[ -n "${SYNC_OUTPUT:-}" ]]; then
        echo "$1" >> "$SYNC_OUTPUT"
    fi
}

ensure_upstream_remote() {
    if ! git remote get-url "$UPSTREAM_REMOTE" >/dev/null 2>&1; then
        git remote add "$UPSTREAM_REMOTE" "$UPSTREAM_URL"
    fi
    git fetch --quiet "$UPSTREAM_REMOTE" "$UPSTREAM_BRANCH"
}

require_branch() {
    local current
    current="$(git rev-parse --abbrev-ref HEAD)"
    if [[ "$current" != "$BRANCH" ]]; then
        echo "Error: run from $BRANCH, not $current" >&2
        exit 1
    fi
    # Tracked files only: a rebase is unsafe with local edits, but an
    # untracked log or the CI's .utils checkout is not local state.
    if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
        echo "Error: tracked files have local changes" >&2
        exit 1
    fi
}

push_mirror() {
    if [[ "$DRY_RUN" -eq 1 ]]; then
        echo "dry-run: would push $UPSTREAM_REMOTE/$UPSTREAM_BRANCH -> $ORIGIN_REMOTE/$MIRROR_BRANCH"
        return
    fi
    git push --quiet "$ORIGIN_REMOTE" \
        "refs/remotes/$UPSTREAM_REMOTE/$UPSTREAM_BRANCH:refs/heads/$MIRROR_BRANCH"
}

rebase_onto_upstream() {
    local tip
    tip="$(git rev-parse "$UPSTREAM_REMOTE/$UPSTREAM_BRANCH")"
    emit "upstream_sha=$tip"
    if git merge-base --is-ancestor "$tip" HEAD; then
        emit "status=current"
        return 1
    fi
    if git rebase --quiet "$tip"; then
        return 0
    fi
    local conflicts
    conflicts="$(git diff --name-only --diff-filter=U | tr '\n' ' ')"
    git rebase --abort
    emit "status=conflict"
    emit "conflicts=$conflicts"
    echo "Rebase onto $tip conflicts in: $conflicts" >&2
    exit 3
}

run_gates() {
    if [[ "${SKIP_GATES:-0}" == "1" ]]; then
        echo "SKIP_GATES=1: gates not run"
        return
    fi
    if ! bash "$REPO_ROOT/scripts/ci_gates.sh"; then
        emit "status=red"
        echo "Gates failed on the rebased tree; nothing pushed." >&2
        exit 4
    fi
}

push_rebased() {
    if [[ "$DRY_RUN" -eq 1 ]]; then
        emit "status=pushed"
        echo "dry-run: would force-push $BRANCH ($(git rev-parse --short HEAD))"
        return
    fi
    git push --quiet --force-with-lease "$ORIGIN_REMOTE" "$BRANCH"
    emit "status=pushed"
}

main() {
    cd "$REPO_ROOT"
    require_branch
    ensure_upstream_remote
    push_mirror
    if ! rebase_onto_upstream; then
        echo "Already on top of upstream; nothing to do."
        return 0
    fi
    run_gates
    push_rebased
    echo "Rebased and pushed $BRANCH onto upstream."
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=1
            shift
            ;;
        -h|--help)
            sed -n '3,22p' "$0"
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

main
