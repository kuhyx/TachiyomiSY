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
#                                      # build inputs (the pre-push hook's mode:
#                                      # under the local resource cap a full
#                                      # check runs well over ten minutes, and a
#                                      # docs-only push must not pay for it)
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
RUN_GRADLE=1
CHANGED_ONLY=0
GRADLE_TASKS="${GRADLE_TASKS:-check}"
#: Paths whose change makes the Gradle gate necessary.
readonly BUILD_INPUTS=('*.kt' '*.kts' '*.java' '*.toml' '*.xml' '*.properties' '*.pro' 'gradlew')

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

banner() {
    echo "== $1"
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
    changed="$(git -C "$REPO_ROOT" diff --name-only "$range" -- "${BUILD_INPUTS[@]}" 2>/dev/null || true)"
    if [[ -z "$changed" ]]; then
        echo "no build inputs changed in $range; skipping the gradle gate"
        return 1
    fi
    echo "build inputs changed in $range:"
    echo "$changed" | sed 's/^/  /' | head -20
}

jitpack_preflight() {
    # JitPack evicts builds it has not served for a while and Gradle then
    # says "Could not find", which reads like a catalog typo. Ask first.
    banner "jitpack artifacts are served"
    python3 "$REPO_ROOT/scripts/jitpack_preflight.py"
}

gradle_gate() {
    banner "gradle $GRADLE_TASKS"
    local ci_jdk="/usr/lib/jvm/java-17-openjdk"
    if [[ -z "${JAVA_HOME:-}" && -d "$ci_jdk" ]]; then
        export JAVA_HOME="$ci_jdk"
    fi
    local capped="$HOME/.claude/scripts/capped.sh"
    local -a tasks
    read -ra tasks <<< "$GRADLE_TASKS"
    # Locally the build runs under the shared resource cap; on a runner there
    # is nothing else to protect and the cap script does not exist.
    if [[ -z "${CI:-}" && -x "$capped" ]]; then
        # Measured 2026-09-12: with the project's default -Xmx4g and parallel
        # workers a full check exceeds the 4 GiB cap and is SIGTERMed; with
        # these limits it peaks at 1.9 GiB. Slower, but it finishes.
        # 2026-09-13: :app:lintAnalyzeDebug alone needs more than the cap
        # leaves next to the daemon and the Kotlin daemon (SIGTERM 143 at
        # 1.5, 2 and 2.5 GiB, in-process or as a worker), so Android Lint is
        # CI's job: the local gate is compile, tests, detekt, ktlint, Kover.
        CAP_MEM=4G CAP_CPU_PCT=20 "$capped" \
            "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "${tasks[@]}" -x lint \
            --max-workers=2 \
            -Dorg.gradle.parallel=false \
            -Dorg.gradle.jvmargs="-Xmx2048m -Dfile.encoding=UTF-8" \
            -Dkotlin.daemon.jvm.options=-Xmx768m
    else
        "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "${tasks[@]}"
    fi
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
