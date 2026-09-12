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
RUN_GRADLE=1
CHANGED_ONLY=0
#: Gradle modules already brought under the 250-line cap. The cap is enforced
#: on everything outside the module directories plus these; a module is
#: appended in the same commit that makes it clean, and the list only grows.
#: Rollout order (AGENTS.md): gradle/build-logic source-api core-metadata
#: core/common domain data presentation-core presentation-widget
#: source-local i18n i18n-sy baseline-profile app.
readonly CAPPED_MODULES=()
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

uncapped_module_pattern() {
    # ERE matching paths under modules NOT yet on the cap, e.g.
    # ^(app|core/common)/ -- a module dir is wherever a build.gradle.kts is.
    local dir name pattern=""
    while IFS= read -r dir; do
        name="${dir#"$REPO_ROOT"/}"
        name="${name%/build.gradle.kts}"
        if [[ " ${CAPPED_MODULES[*]:-} " == *" $name "* ]]; then
            continue
        fi
        pattern="${pattern:+$pattern|}$name"
    done < <(find "$REPO_ROOT" -mindepth 2 -maxdepth 4 -name build.gradle.kts -not -path '*/build/*' | sort)
    echo "^($pattern)/"
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
        CAP_MEM=4G CAP_CPU_PCT=20 "$capped" \
            "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "${tasks[@]}"
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
