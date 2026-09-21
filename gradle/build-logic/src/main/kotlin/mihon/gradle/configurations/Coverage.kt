package mihon.gradle.configurations

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import mihon.gradle.extensions.isInGateScope
import mihon.gradle.extensions.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File

private const val FULL_COVERAGE: Int = 100
private const val ANDROID_APPLICATION_PLUGIN: String = "com.android.application"

/**
 * Kover verification at 100% of lines and branches; `check` fails below it. Skipped, like the
 * tests it reads, outside the gate's scope ([mihon.gradle.extensions.GATE_MODULES_PROPERTY]).
 *
 * The JaCoCo engine is used on purpose: its Kotlin filters drop the branches the
 * compiler and kotlinx.serialization generate with no reachable outcome (default
 * masks, `0 & seen` field checks), which the IntelliJ agent counts as missed.
 *
 * An application module is verified on its `debug` variant (`koverVerifyDebug`): the total
 * variant would merge every build type -- release, foss and the benchmark pair -- which means
 * compiling the app four more times only to read the same classes with no test data. A library
 * keeps the total variant (its release classes are compiled for the AAR anyway).
 *
 * Two annotations from core/common are the only exclusions hand-written code gets:
 * `NativeBinding` (code that cannot run on the JVM) and `InlinedOnly` (reified inline stubs
 * that only throw); see their KDoc. A module without core/common on its classpath simply
 * matches nothing. Code the Android toolchain generates is not measured either: `BuildConfig`,
 * ViewBinding classes (`*.databinding.*`) and the AIDL stubs of every `.aidl` under `src/main/aidl`
 * (Kover has no notion of generated sources; `androidGeneratedClasses()` would also drop every
 * `*Activity`/`*Fragment`, which is real code here).
 */
public fun Project.configureCoverage() {
    val isInScope = isInGateScope()
    val verifyTask = if (plugins.hasPlugin(ANDROID_APPLICATION_PLUGIN)) "koverVerifyDebug" else "koverVerify"
    tasks.matching { it.name == "check" }.configureEach { dependsOn(verifyTask) }
    tasks.matching { it.name == verifyTask }.configureEach {
        onlyIf("the module is in the gate's scope") { isInScope }
    }
    extensions.configure<KoverProjectExtension> {
        useJacoco(libs.versions.jacoco.get())
        reports {
            filters {
                excludes {
                    annotatedBy("mihon.core.common.NativeBinding", "mihon.core.common.InlinedOnly")
                    classes("*.BuildConfig", "*.databinding.*")
                    classes(aidlGeneratedClassesWithNested())
                }
            }
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

private fun Project.aidlGeneratedClassesWithNested(): List<String> {
    val aidlRoot = layout.projectDirectory.dir("src/main/aidl").asFile
    return aidlRoot.walkTopDown()
        .filter { it.isFile && it.extension == "aidl" }
        .map { it.relativeTo(aidlRoot).path.removeSuffix(".aidl").replace(File.separatorChar, '.') }
        .flatMap { listOf(it, "$it$*") }
        .toList()
}
