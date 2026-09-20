package mihon.gradle.extensions

import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.accessors.dm.LibrariesForMihonx
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the

internal val Project.libs get() = the<LibrariesForLibs>()
internal val Project.mihonx get() = the<LibrariesForMihonx>()

internal fun Project.plugins(block: PluginManager.() -> Unit) {
    pluginManager.apply(block)
}

internal fun Project.android(block: CommonExtension.() -> Unit) {
    extensions.configure(block)
}

/**
 * JUnit Platform for every test task, logging each outcome; skipped outside the gate's scope
 * ([GATE_MODULES_PROPERTY]).
 *
 * Only `*Test` classes are offered to the engines: the platform gets every class in the
 * test output otherwise, and the vintage engine reflects over each one. A Robolectric
 * shadow subclass (`ShadowView` names `ViewRootImpl$CalledFromWrongThreadException`,
 * hidden from android.jar) fails that reflection outside the sandbox.
 */
public fun Project.configureTest() {
    val isInScope = isInGateScope()
    tasks.withType(Test::class.java).configureEach {
        onlyIf("the module is in the gate's scope") { isInScope }
        useJUnitPlatform()
        include("**/*Test.class")
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}
