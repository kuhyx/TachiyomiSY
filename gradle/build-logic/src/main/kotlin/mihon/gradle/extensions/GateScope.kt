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
