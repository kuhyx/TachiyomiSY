#!/bin/bash

# ============================================================================
# The Gradle half of scripts/ci_gates.sh, sourced by it: `gradle_gate` runs
# $GRADLE_TASKS (default `check`) the way the current machine can afford.
#
# Needs from the caller: REPO_ROOT, GRADLE_TASKS, CHANGED_ONLY,
# GATE_MODULES_PROPERTY, and the `banner` and `gate_modules` functions.
# ============================================================================

#: build-logic's GATE_PHASE_PROPERTY / STATIC_PHASE: skip tests and coverage,
#: build everything they depend on.
readonly GATE_PHASE_PROPERTY="mihon.gate.phase"
readonly STATIC_PHASE="static"
#: build-logic's TEST_FORKS_PROPERTY: test JVMs per test task.
readonly TEST_FORKS_PROPERTY="mihon.test.forks"
#: Test JVMs per test task on a runner (4 cores, 16 GiB, nothing else on it).
readonly CI_TEST_FORKS=3

# Locally the gate shares ~/.claude's capped.slice (8 GiB for ALL capped jobs
# together) with whatever else is running, so it has to fit next to them.
# One `check` holding compilation, lint and four Robolectric test JVMs at once
# peaked at 6.5 GiB and was OOM-killed on 2026-09-26 while another session's
# flutter check held 2.8 GiB. So the same `check` runs twice:
#
#   phase 1  -P$GATE_PHASE_PROPERTY=$STATIC_PHASE: every test and Kover task
#            is skipped (onlyIf), so what they depend on -- main and test
#            compilation -- still builds, next to detekt, spotless and lint.
#   phase 2  the plain `check`: all of that is up to date, only tests and
#            coverage execute, one test JVM at a time.
#
# Phase 2 is exactly what CI runs, so the gate cannot drop a task. The
# Kotlin daemon arguments are pinned in both runs: they are inputs of the
# Kotlin compile tasks and are otherwise inherited from org.gradle.jvmargs,
# so a different daemon heap per phase would recompile all of :app. With
# the pin each phase can size its own (single-use) daemon. Measured
# 2026-09-26 next to a 2.8 GiB job, full :app recompile plus lint of every
# module: -Xmx1536m thrashed in GC; -Xmx3g passed at 4.65 GiB RSS but the
# slice OOM-killed the other job; -Xmx2g with the serial collector passed at
# 3.5 GiB RSS in 507 s. Phase 2 only orchestrates the test JVMs.
local_gradle_run() {
    local capped="$1" heap="$2"; shift 2
    CAP_MEM=5G CAP_CPU_PCT="$(sed -n 's/^readonly HARD_CPU_PCT=\([0-9]*\)$/\1/p' "$capped")" \
        "$capped" "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "$@" \
        --max-workers=1 \
        -Dorg.gradle.parallel=false \
        -Dorg.gradle.jvmargs="-Xmx$heap -XX:+UseSerialGC -Dfile.encoding=UTF-8" \
        -Pkotlin.daemon.jvmargs=-Xmx2g \
        -Pkotlin.compiler.execution.strategy=in-process
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
        banner "gradle phase 1/2: $GRADLE_TASKS without tests (compile, lint, detekt, spotless)"
        local_gradle_run "$capped" 2g "${tasks[@]}" "${scope[@]}" "-P$GATE_PHASE_PROPERTY=$STATIC_PHASE"
        banner "gradle phase 2/2: $GRADLE_TASKS (tests and coverage)"
        local_gradle_run "$capped" 1g "${tasks[@]}" "${scope[@]}"
    else
        # A GitHub runner has 4 cores and 16 GiB and runs one build: give
        # the daemon (R8 runs inside it) half the machine.
        "$REPO_ROOT/gradlew" -p "$REPO_ROOT" "${tasks[@]}" \
            "-P$TEST_FORKS_PROPERTY=$CI_TEST_FORKS" \
            -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8"
    fi
}
