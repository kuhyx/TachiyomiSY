package mihon.gradle.extensions

import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.accessors.dm.LibrariesForMihonx
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
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

private const val TEST_HEAP: String = "1536m"
private const val TEST_CLASSES_PER_FORK: Long = 200

/**
 * JUnit Platform for every test task, logging each outcome; skipped outside the gate's scope
 * ([GATE_MODULES_PROPERTY]).
 *
 * Only `*Test` classes are offered to the engines: the platform gets every class in the
 * test output otherwise, and the vintage engine reflects over each one. A Robolectric
 * shadow subclass (`ShadowView` names `ViewRootImpl$CalledFromWrongThreadException`,
 * hidden from android.jar) fails that reflection outside the sandbox.
 *
 * The test JVM's `java.io.tmpdir` is the task's own `build/tmp/<task>`: Robolectric extracts
 * its native runtime (fonts, ICU data, `libandroid_runtime.so`, ~100 MiB) there once per
 * sandbox and only deletes it at JVM exit. On `/tmp` (tmpfs) a whole-tree run held 3.1 GiB
 * of that as unswappable shared memory inside the 8 GiB local cap and was OOM-killed
 * (2026-09-20); on disk it is page cache the kernel can drop.
 *
 * Tests run on the `test-jdk` toolchain (21) while the code still compiles to `java` (17):
 * `multiplatform-markdown-renderer` ships Java 21 bytecode, which a JDK 17 test JVM cannot load
 * (`UnsupportedClassVersionError` in every test that composes `Markdown`). On a device D8
 * handles it; only the host JVM needed the newer runtime (decided 2026-09-26).
 */
public fun Project.configureTest() {
    val isInScope = isInGateScope()
    pluginManager.apply(JavaBasePlugin::class.java)
    val testLauncher = extensions.getByType(JavaToolchainService::class.java).launcherFor {
        languageVersion.set(JavaLanguageVersion.of(mihonx.versions.test.jdk.get()))
    }
    tasks.withType(Test::class.java).configureEach {
        javaLauncher.set(testLauncher)
        // Robolectric keeps per-test state alive, so one JVM running every `app` test outgrew Gradle's
        // 512 MiB default (OutOfMemoryError from ~2800 tests, 2026-09-26). A fresh JVM every
        // TEST_CLASSES_PER_FORK classes bounds that growth however large the suite gets.
        maxHeapSize = TEST_HEAP
        forkEvery = TEST_CLASSES_PER_FORK
        onlyIf("the module is in the gate's scope") { isInScope }
        useJUnitPlatform()
        include("**/*Test.class")
        systemProperty("java.io.tmpdir", temporaryDir.absolutePath)
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
}
