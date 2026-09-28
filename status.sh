#!/bin/bash

# ============================================================================
# Where the app coverage campaign stands: coverage, lint, the 250-line cap,
# tests, worktrees, CI and the finish-line checklist, in about a second.
#
# Installs what the report needs (Arch: pacman --needed), then runs
# scripts/campaign_status.py. The report never runs Gradle in this tree; when
# its cached coverage/lint is older than the tree it starts one background
# refresh in the sibling worktree <repo>-status (memory-capped, one at a time).
#
# Usage: ./status.sh [--top N]
# ============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT
readonly REPORT="$REPO_ROOT/scripts/campaign_status.py"

# command -> Arch package that provides it
readonly -A PACKAGES=(
    ["python3"]=python
    ["git"]=git
    ["gh"]=github-cli
    ["rsync"]=rsync
    ["systemd-run"]=systemd
)
# JDKs the background refresh needs: Gradle runs on 17, the unit tests on 21.
readonly -A JDKS=(
    [/usr/lib/jvm/java-17-openjdk]=jdk17-openjdk
    [/usr/lib/jvm/java-21-openjdk]=jdk21-openjdk
)

missing_packages() {
    local cmd dir
    for cmd in "${!PACKAGES[@]}"; do
        command -v "$cmd" >/dev/null 2>&1 || echo "${PACKAGES[$cmd]}"
    done
    for dir in "${!JDKS[@]}"; do
        [[ -x "$dir/bin/java" ]] || echo "${JDKS[$dir]}"
    done
}

install_dependencies() {
    local -a needed
    mapfile -t needed < <(missing_packages | sort -u)
    ((${#needed[@]} == 0)) && return 0
    if ! command -v pacman >/dev/null 2>&1; then
        echo "Error: missing ${needed[*]} and no pacman to install them with" >&2
        exit 1
    fi
    echo "Installing: ${needed[*]}"
    sudo pacman -S --needed --noconfirm "${needed[@]}"
}

check_github_login() {
    # Without a login the report still runs; only its CI lines stay empty.
    if ! gh auth status >/dev/null 2>&1; then
        echo "Note: gh is not logged in, so CI status is skipped (run: gh auth login)" >&2
    fi
}

main() {
    install_dependencies
    check_github_login
    exec python3 "$REPORT" "$@"
}

main "$@"
