package mihon.gradle.configurations

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import mihon.gradle.extensions.isInGateScope
import mihon.gradle.extensions.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

private const val FULL_COVERAGE: Int = 100

/**
 * Kover verification at 100% of lines and branches; `check` fails below it. Skipped, like the
 * tests it reads, outside the gate's scope ([mihon.gradle.extensions.GATE_MODULES_PROPERTY]).
 *
 * The JaCoCo engine is used on purpose: its Kotlin filters drop the branches the
 * compiler and kotlinx.serialization generate with no reachable outcome (default
 * masks, `0 & seen` field checks), which the IntelliJ agent counts as missed.
 */
public fun Project.configureCoverage() {
    val isInScope = isInGateScope()
    tasks.matching { it.name == "check" }.configureEach { dependsOn("koverVerify") }
    tasks.matching { it.name == "koverVerify" }.configureEach {
        onlyIf("the module is in the gate's scope") { isInScope }
    }
    extensions.configure<KoverProjectExtension> {
        useJacoco(libs.versions.jacoco.get())
        reports {
            verify {
                rule("line coverage") {
                    minBound(FULL_COVERAGE, CoverageUnit.LINE)
                }
                rule("branch coverage") {
                    minBound(FULL_COVERAGE, CoverageUnit.BRANCH)
                }
            }
        }
    }
}
