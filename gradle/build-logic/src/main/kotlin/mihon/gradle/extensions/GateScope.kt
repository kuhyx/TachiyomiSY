package mihon.gradle.extensions

import org.gradle.api.Project

/**
 * The Gradle property that narrows a `check` run to a few modules: a comma-separated list of
 * project paths (`:core:common,:source-local`). The pre-push gate sets it to the modules the
 * pushed commits touch so only their tests and coverage run; CI never sets it and runs
 * everything. Compilation is never narrowed, so a consumer of a changed module still builds.
 */
public const val GATE_MODULES_PROPERTY: String = "mihon.gate.modules"

/** Whether this project's tests belong in the current run; always true without the property. */
public fun Project.isInGateScope(): Boolean {
    val scope = findProperty(GATE_MODULES_PROPERTY)?.toString() ?: return true
    return path in scope.split(',').map(String::trim)
}

/**
 * The Gradle property that splits the local gate into two runs of the same `check`: with
 * [STATIC_PHASE] every test and coverage task is skipped while everything they depend on --
 * main and test compilation -- still builds, next to detekt, spotless and lint. The second run
 * then finds all of that up to date and only tests and coverage execute, so neither run needs the
 * memory of both at once. CI never sets it.
 */
public const val GATE_PHASE_PROPERTY: String = "mihon.gate.phase"

/** The [GATE_PHASE_PROPERTY] value of the gate's first, test-free run. */
public const val STATIC_PHASE: String = "static"

/** Whether this project's tests and coverage run now: in the gate's scope, outside its static phase. */
public fun Project.gateRunsTests(): Boolean =
    isInGateScope() && findProperty(GATE_PHASE_PROPERTY)?.toString() != STATIC_PHASE
