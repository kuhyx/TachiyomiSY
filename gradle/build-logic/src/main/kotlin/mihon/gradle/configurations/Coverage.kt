package mihon.gradle.configurations

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

private const val FULL_COVERAGE: Int = 100

/** Kover verification at 100% of lines and branches; `koverVerify` fails below it. */
public fun Project.configureCoverage() {
    extensions.configure<KoverProjectExtension> {
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
