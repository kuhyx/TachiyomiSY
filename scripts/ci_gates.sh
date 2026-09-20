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
#                                      # a full check is ~2 min under the local
#                                      # cap, and a docs-only push must not pay
#                                      # for it)
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
RUN_GRADLE=1
CHANGED_ONLY=0
GRADLE_TASKS="${GRADLE_TASKS:-check}"
#: Paths whose change makes the Gradle gate necessary.
readonly BUILD_INPUTS=('*.kt' '*.kts' '*.java' '*.toml' '*.xml' '*.properties' '*.pro' 'gradlew')
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
    local scope=()
    if [[ "$CHANGED_ONLY" -eq 1 ]]; then
        local modules
        modules="$(gate_modules)"
        if [[ -n "$modules" ]]; then
            echo "  tests and coverage scoped to: $modules (CI runs every module)"
            scope=("-P$GATE_MODULES_PROPERTY=$modules")
        fi
    fi
    if [[ -z "${CI:-}" && -x "$capped" ]]; then
        # The cap is whatever ceiling capped.sh currently allows, read from
        # the script so a raised ceiling speeds the gate up without a second
        # edit here and a lowered one cannot make it refuse to run.
        # 2026-09-13: :app:lintAnalyzeDebug alone needs more than the cap
        # leaves next to the daemon and the Kotlin daemon (SIGTERM 143 at
        # 1.5, 2 and 2.5 GiB, in-process or as a worker), so Android Lint is
        # CI's job: the local gate is compile, tests, detekt, ktlint, Kover.
        local mem_g cpu_pct workers
        mem_g="$(sed -n 's/^readonly HARD_MEM_G=\([0-9]*\)$/\1/p' "$capped")"
        cpu_pct="$(sed -n 's/^readonly HARD_CPU_PCT=\([0-9]*\)$/\1/p' "$capped")"
        : "${mem_g:=4}" "${cpu_pct:=20}"
        workers=$(( $(nproc) * cpu_pct / 100 ))
        (( workers < 2 )) && workers=2
        # Every worker may be a Robolectric test JVM (~1 GiB each next to the
        # daemon), so with six such modules 12 workers overran the 8 GiB cap
        # (SIGTERM 143 on 2026-09-19 once source-local's suite joined); four
        # keeps the peak under it.
        (( workers > 4 )) && workers=4
        if (( mem_g >= 8 )); then
            # 8 GiB and up: parallel project execution, one worker per capped
            # core, a quarter of the cap for the daemon heap. Measured
            # 2026-09-20 on the whole tree (every test task executing): four
            # Robolectric test JVMs peak at ~1.8 GiB RSS each next to the
            # daemon, and a 3 GiB daemon heap left ~0.8 GiB of headroom at
            # 8 GiB; 2 GiB is what the < 8 GiB branch below already compiles
            # the app with.
            CAP_MEM="${mem_g}G" CAP_CPU_PCT="$cpu_pct" "$capped" \
                "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "${tasks[@]}" -x lint "${scope[@]}" \
                --max-workers="$workers" \
                -Dorg.gradle.parallel=true \
                -Dorg.gradle.jvmargs="-Xmx$((mem_g / 4))g -Dfile.encoding=UTF-8" \
                -Dkotlin.daemon.jvm.options=-Xmx1024m
        else
            # Measured 2026-09-12: with the project's default -Xmx4g and
            # parallel workers a full check exceeds a 4 GiB cap and is
            # SIGTERMed; with these limits it peaks at 1.9 GiB. Slower, but
            # it finishes.
            CAP_MEM="${mem_g}G" CAP_CPU_PCT="$cpu_pct" "$capped" \
                "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "${tasks[@]}" -x lint "${scope[@]}" \
                --max-workers=2 \
                -Dorg.gradle.parallel=false \
                -Dorg.gradle.jvmargs="-Xmx2048m -Dfile.encoding=UTF-8" \
                -Dkotlin.daemon.jvm.options=-Xmx768m
        fi
    else
        # A GitHub runner has 4 cores and 16 GiB and runs one build: give
        # the daemon (R8 runs inside it) half the machine.
        # :app:lint is upstream's `abortOnError = false` lint: ~3 min of a
        # 4-core run for a report nothing reads and that cannot fail. It
        # leaves `check` until the app module joins the strict lint stack
        # (build-logic Lint.kt) -- the grep below fails the gate the day
        # that happens, so this exclusion cannot outlive its reason.
        if ! grep -qE '^\s*abortOnError = false' "$REPO_ROOT/app/build.gradle.kts"; then
            echo "app lint is now strict: drop '-x :app:lint' from $0" >&2
            exit 1
        fi
        "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "${tasks[@]}" -x :app:lint \
            -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8"
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
