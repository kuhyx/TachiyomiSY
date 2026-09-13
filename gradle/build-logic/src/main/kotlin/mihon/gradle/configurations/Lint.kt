package mihon.gradle.configurations

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import java.io.File

private const val JVM_TARGET: String = "17"

/**
 * Every detekt rule on, no baseline, one config directory for the whole tree
 * (one file per rule set so each stays under the 250-line cap). [configDir]
 * is passed in because the included build-logic build has a different root
 * than the modules it configures.
 */
public fun Project.configureDetekt(configDir: File) {
    extensions.configure<DetektExtension> {
        allRules = true
        buildUponDefaultConfig = false
        parallel = true
        config.setFrom(fileTree(configDir) { include("*.yml") })
        source.setFrom(layout.projectDirectory.dir("src"))
    }
    tasks.withType(Detekt::class.java).configureEach {
        jvmTarget = JVM_TARGET
        exclude("**/build/**")
        reports {
            html.required.set(false)
            xml.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
            txt.required.set(true)
        }
    }
}

/** Warnings are errors and every public declaration is spelled out. */
public fun Project.configureStrictKotlin() {
    val kotlin = extensions.findByType(KotlinBaseExtension::class.java) ?: return
    if (kotlin is HasConfigurableKotlinCompilerOptions<*>) {
        kotlin.compilerOptions {
            allWarningsAsErrors.set(true)
            freeCompilerArgs.add("-Xexplicit-api=strict")
        }
    }
}

/** Android Lint at its strictest; a no-op on modules without the Android plugin. */
public fun Project.configureAndroidLint() {
    val android = extensions.findByType(CommonExtension::class.java) ?: return
    strictLint(android.lint)
}

/** The same knobs for the KMP Android target, whose lint block hangs off the target. */
public fun strictLint(lint: Lint) {
    lint.warningsAsErrors = true
    lint.abortOnError = true
    lint.checkAllWarnings = true
    lint.checkDependencies = true
    lint.checkReleaseBuilds = true
}
