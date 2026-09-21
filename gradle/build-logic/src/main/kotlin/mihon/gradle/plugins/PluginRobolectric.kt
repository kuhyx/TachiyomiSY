package mihon.gradle.plugins

import com.android.build.api.dsl.CommonExtension
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.testImplementation
import mihon.gradle.extensions.testRuntimeOnly
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Host tests under Robolectric for an Android module: the real android.jar behaviour with the
 * module's resources, Compose's test rule (JUnit 4, run by the vintage engine next to Jupiter),
 * MockWebServer for network fixtures and kotlin-reflect for calling deprecated members.
 *
 * Pair it with `src/test/resources/robolectric.properties` (`sdk`, and `application` for an
 * app module so the real `Application` is not booted per sandbox).
 */
public class PluginRobolectric : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure(CommonExtension::class.java) {
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            dependencies {
                testImplementation(libs.robolectric)
                testImplementation(libs.junit4)
                testRuntimeOnly(libs.junit.vintage)
                testImplementation(libs.androidx.compose.uiTestJunit4)
                testImplementation(libs.androidx.compose.uiTestManifest)
                testImplementation(libs.okhttp.mockwebserver)
                testImplementation(libs.kotlin.reflect)
                testImplementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
