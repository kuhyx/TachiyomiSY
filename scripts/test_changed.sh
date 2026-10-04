#!/bin/bash

# ============================================================================
# Run only the tests related to files changed vs HEAD (staged, unstaged and
# untracked). Quiet: failures plus a one-line summary. A changed file maps to
# the Gradle module that owns it (its unit-test task); changed scripts/*.py run
# the scripts' pytest; anything unmappable (root build files, version
# catalogs, build-logic) falls back to the full unit-test run.
# ============================================================================

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"
# shellcheck source=scripts/capped_modules.sh
source "$REPO_ROOT/scripts/capped_modules.sh"

# Same JDK fallback as gradle_gate.sh: the default system JDK is newer than what Kotlin targets.
if [[ -z "${JAVA_HOME:-}" && -d /usr/lib/jvm/java-17-openjdk ]]; then
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
fi

CHANGED=()
while IFS= read -r f; do
    [[ -n "$f" && -e "$f" ]] && CHANGED+=("$f")
done < <({ git diff --name-only HEAD 2>/dev/null || true; git ls-files --others --exclude-standard; } | sort -u)

full=0
py=0
tasks=()
dirs="$(module_dirs)"
for f in "${CHANGED[@]:-}"; do
    [[ -z "$f" ]] && continue
    case "$f" in
        scripts/*.py) py=1; continue ;;
        scripts/* | *.md | .github/* | .pre-commit-config.yaml | .gitignore) continue ;;
        gradle/build-logic/*) full=1; continue ;;
    esac
    if [[ "$f" != */src/* ]]; then
        # Not under a module's src/: only a build file can matter, and at the root it affects all.
        case "$f" in *.kts | *.gradle | *.toml | gradle.properties) ;; *) continue ;; esac
    fi
    module=""
    for dir in $dirs; do
        if [[ "$f" == "$dir"/* ]]; then module="$dir"; break; fi
    done
    if [[ -z "$module" ]]; then full=1; continue; fi
    # KMP modules name their JVM test task testAndroidHostTest, Android ones testDebugUnitTest;
    # a module with no test source set (i18n) has nothing to run.
    if grep -q multiplatform "$module/build.gradle.kts"; then
        [[ -d "$module/src/androidHostTest" ]] || continue
        task=":${module//\//:}:testAndroidHostTest"
    else
        [[ -d "$module/src/test" ]] || continue
        task=":${module//\//:}:testDebugUnitTest"
    fi
    [[ " ${tasks[*]:-} " == *" $task "* ]] || tasks+=("$task")
done

if [[ $py -eq 1 ]]; then python3 -m pytest -q scripts; fi
if [[ $full -eq 1 ]]; then
    echo "unmapped change: running the full suite"
    exec ./gradlew testDebugUnitTest testAndroidHostTest --quiet
fi
if [[ ${#tasks[@]} -gt 0 ]]; then
    ./gradlew "${tasks[@]}" --quiet
elif [[ $py -eq 0 ]]; then
    echo "no jvm source changes: nothing to test"
fi
