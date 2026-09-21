package mihon.gradle.plugins

import com.android.build.api.dsl.CommonExtension
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.testImplementation
import mihon.gradle.extensions.testRuntimeOnly
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

private const val ANDROID_APPLICATION_PLUGIN: String = "com.android.application"

/**
 * Host tests under Robolectric for an Android module: the real android.jar behaviour with the
 * module's resources, Compose's test rule (JUnit 4, run by the vintage engine next to Jupiter),
 * MockWebServer for network fixtures and kotlin-reflect for calling deprecated members.
 *
 * Pair it with `src/test/resources/robolectric.properties` (`sdk`, and `application` for an
 * app module so the real `Application` is not booted per sandbox).
 *
 * An application module also gets `ui-test-manifest` as `debugImplementation`: Robolectric reads
 * the app's own merged debug manifest, where a `testImplementation` manifest never lands, so
 * `createComposeRule()` could not resolve its `ComponentActivity` (a library merges test
 * manifests; the app does not).
 *
 * `conscrypt-android` (okhttp's platform on a device) is dropped from every unit-test runtime
 * classpath: it carries the same `org.conscrypt` classes as Robolectric's `conscrypt-openjdk-uber`
 * without the JVM native library, and shadows it, so every sandbox died with
 * `UnsatisfiedLinkError: no conscrypt_jni` (2026-09-21).
 */
public class PluginRobolectric : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure(CommonExtension::class.java) {
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
                exclude(mapOf("group" to "org.conscrypt", "module" to "conscrypt-android"))
            }
            dependencies {
                testImplementation(libs.robolectric)
                testImplementation(libs.junit4)
                testRuntimeOnly(libs.junit.vintage)
                testImplementation(libs.androidx.compose.uiTestJunit4)
                testImplementation(libs.androidx.compose.uiTestManifest)
                if (plugins.hasPlugin(ANDROID_APPLICATION_PLUGIN)) {
                    add("debugImplementation", libs.androidx.compose.uiTestManifest)
                }
                testImplementation(libs.okhttp.mockwebserver)
                testImplementation(libs.kotlin.reflect)
                testImplementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
