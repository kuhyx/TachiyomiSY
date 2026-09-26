#!/bin/bash

# ============================================================================
# Every gate this fork enforces, in one place.
#
# Run by CI on every push, by the daily upstream sync before it force-pushes,
# and by the pre-push hook locally -- so "green locally, red in CI" has no
# diff to hide in. Add a gate HERE, never in a workflow file alone.
#
# Usage:
#   scripts/ci_gates.sh                # all gates
#   scripts/ci_gates.sh --no-gradle    # only the shell gates (seconds, not minutes)
#   scripts/ci_gates.sh --changed-only # gradle only if the push range touches
#                                      # build inputs, scoped to the touched
#                                      # modules (the pre-push hook's mode, and
#                                      # the per-commit gate to run detached:
#                                      # ~65 s of check plus ~3 min of Android
#                                      # Lint when app changed; a docs-only
#                                      # push must not pay for it)
#
# Env:
#   UTILS_ROOT     where github.com/kuhyx/utils is checked out
#                  (default: .utils if present, else ~/src/utils)
#   GRADLE_TASKS   Gradle tasks that constitute the build gate
#                  (default: "check")
#   JAVA_HOME      unset locally: falls back to the JDK CI pins (17) when it
#                  is installed, so a local run mirrors the runner
# ============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT
# shellcheck source=scripts/capped_modules.sh
source "$REPO_ROOT/scripts/capped_modules.sh"
# shellcheck source=scripts/gate_modules.sh
source "$REPO_ROOT/scripts/gate_modules.sh"
# shellcheck source=scripts/gradle_gate.sh
source "$REPO_ROOT/scripts/gradle_gate.sh"
RUN_GRADLE=1
CHANGED_ONLY=0
GRADLE_TASKS="${GRADLE_TASKS:-check}"
#: Paths whose change makes the Gradle gate necessary.
#: The gate's own Gradle half counts: a change to how it runs must run it.
readonly BUILD_INPUTS=('*.kt' '*.kts' '*.java' '*.toml' '*.xml' '*.properties' '*.pro' 'gradlew'
    'scripts/ci_gates.sh' 'scripts/gradle_gate.sh')
readonly GATE_MODULES_PROPERTY="mihon.gate.modules"

resolve_utils_root() {
    if [[ -n "${UTILS_ROOT:-}" ]]; then
        return
    fi
    if [[ -d "$REPO_ROOT/.utils" ]]; then
        UTILS_ROOT="$REPO_ROOT/.utils"
    else
        UTILS_ROOT="$HOME/src/utils"
    fi
    export UTILS_ROOT
}

# Each banner also reports how long the previous step took, so a slow push
# says which gate to look at (the JitPack preflight was 17 s of a 114 s run
# before it was parallelised, and nobody could tell without this).
STEP_STARTED=$SECONDS
banner() {
    local now=$SECONDS
    echo "== $1  (previous step $((now - STEP_STARTED))s)"
    STEP_STARTED=$now
}

shell_gates() {
    banner "file length <= 250 lines (root + capped modules)"
    local skip
    skip="$(uncapped_module_pattern)"
    echo "  not yet capped: $skip"
    git -C "$REPO_ROOT" ls-files -z |
        grep -zvE "$skip" |
        xargs -0 bash "$REPO_ROOT/scripts/check_file_length.sh"

    banner "markdown naming"
    bash "$REPO_ROOT/scripts/check_md_naming.sh" --all

    banner "no binaries outside .binary-allowlist"
    git -C "$REPO_ROOT" ls-files -z |
        xargs -0 bash "$REPO_ROOT/scripts/check_no_binaries.sh"

    banner "dependencies on newest stable"
    # --strict only where a network is guaranteed: offline the gate degrades
    # to its cached answer, and a hook that hard-failed on a flaky connection
    # would leave no way to push, since --no-verify is banned.
    local strict=()
    if [[ -n "${CI:-}" ]]; then
        strict=(--strict)
    fi
    bash "$REPO_ROOT/scripts/check_dependency_freshness.sh" --all "${strict[@]}"
}

push_range() {
    # pre-commit exports the pushed range; by hand, everything not on origin.
    if [[ -n "${PRE_COMMIT_FROM_REF:-}" && -n "${PRE_COMMIT_TO_REF:-}" ]]; then
        echo "$PRE_COMMIT_FROM_REF..$PRE_COMMIT_TO_REF"
    else
        echo "origin/master..HEAD"
    fi
}

build_inputs_changed() {
    local range changed
    range="$(push_range)"
    # Fail closed: a diff that errors must run the gate, never skip it.
    if ! changed="$(git -C "$REPO_ROOT" diff --name-only "$range" -- "${BUILD_INPUTS[@]}")"; then
        echo "cannot diff $range; running the gradle gate"
        return 0
    fi
    if [[ -z "$changed" ]]; then
        echo "no build inputs changed in $range; skipping the gradle gate"
        return 1
    fi
    echo "build inputs changed in $range:"
    # No `| head` here: under pipefail its early exit SIGPIPEs sed, the
    # function returned 141, and the caller read that as "skip gradle" --
    # a 454-file push went through unchecked on 2026-09-26.
    printf '%s\n' "$changed" | sed -n '1,20s/^/  /p'
    return 0
}

jitpack_preflight() {
    # JitPack evicts builds it has not served for a while and Gradle then
    # says "Could not find", which reads like a catalog typo. Ask first.
    banner "jitpack artifacts are served"
    python3 "$REPO_ROOT/scripts/jitpack_preflight.py"
}

main() {
    cd "$REPO_ROOT"
    resolve_utils_root
    shell_gates
    if [[ "$CHANGED_ONLY" -eq 1 ]] && ! build_inputs_changed; then
        RUN_GRADLE=0
    fi
    if [[ "$RUN_GRADLE" -eq 1 ]]; then
        jitpack_preflight
        gradle_gate
    fi
    banner "all gates green"
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-gradle)
            RUN_GRADLE=0
            shift
            ;;
        --changed-only)
            CHANGED_ONLY=1
            shift
            ;;
        -h|--help)
            sed -n '3,26p' "$0"
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

main
