package mihon.gradle.configurations

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import mihon.gradle.tasks.ReplaceShortcutsPlaceholderTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** The ABIs the application ships, plus a universal APK. */
public val APP_ABIS: List<String> = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

/** Native libraries whose debug symbols stay in the APK. */
public val KEPT_DEBUG_SYMBOLS: List<String> = listOf(
    "libandroidx.graphics.path",
    "libarchive-jni",
    "libconscrypt_jni",
    "libimagedecoder",
    "libquickjs",
    "libsqlite3x",
)

/** Packaged resources the APK does not need. */
public val EXCLUDED_RESOURCES: Set<String> = setOf(
    "kotlin-tooling-metadata.json",
    "LICENSE.txt",
    "META-INF/**/*.properties",
    "META-INF/**/LICENSE.txt",
    "META-INF/*.properties",
    "META-INF/*.version",
    "META-INF/DEPENDENCIES",
    "META-INF/INDEX.LIST",
    "META-INF/LICENSE",
    "META-INF/NOTICE",
    "META-INF/README.md",
)

/** Experimental APIs the application opts into project-wide. */
public val APP_OPT_INS: List<String> = listOf(
    "androidx.compose.animation.ExperimentalAnimationApi",
    "androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
    "androidx.compose.foundation.ExperimentalFoundationApi",
    "androidx.compose.foundation.layout.ExperimentalLayoutApi",
    "androidx.compose.material3.ExperimentalMaterial3Api",
    "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
    "androidx.compose.ui.ExperimentalComposeUiApi",
    "coil3.annotation.ExperimentalCoilApi",
    "com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi",
    "kotlinx.coroutines.ExperimentalCoroutinesApi",
    "kotlinx.coroutines.FlowPreview",
    "kotlinx.coroutines.InternalCoroutinesApi",
    "kotlinx.serialization.ExperimentalSerializationApi",
)

/**
 * The application module's packaging: ABI splits, kept debug symbols, excluded resources, the
 * extra source sets, build features, the compiler opt-ins and the per-variant `shortcuts.xml`
 * placeholder task. Upstream keeps all of this inline in `app/build.gradle.kts`; it lives here
 * so that script stays under the 250-line cap.
 */
public fun Project.configureAppPackaging() {
    extensions.configure<ApplicationExtension> {
        sourceSets {
            getByName("release").java.directories.add("src/release/java")
            getByName("debug").java.directories.add("src/debug/java")
        }
        splits.abi {
            isEnable = true
            isUniversalApk = true
            reset()
            APP_ABIS.forEach { include(it) }
        }
        packaging {
            jniLibs.keepDebugSymbols += KEPT_DEBUG_SYMBOLS.map { "**/$it.so" }
            resources.excludes += EXCLUDED_RESOURCES
        }
        dependenciesInfo.includeInApk = false
        buildFeatures {
            viewBinding = true
            buildConfig = true
            aidl = true
        }
        // The app is last in the rollout order, so it re-checks every library it depends on.
        lint.checkDependencies = true
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions.freeCompilerArgs.addAll(APP_OPT_INS.map { "-opt-in=$it" })
    }
    extensions.configure<ApplicationAndroidComponentsExtension> {
        onVariants { variant -> registerShortcutsTask(variant) }
        // Only excluding in the standard flavor because this breaks Layout Inspector's Compose tree.
        onVariants(selector().withFlavor("default" to "standard")) {
            it.packaging.resources.excludes.add("META-INF/*.version")
        }
    }
}

/** Feeds `shortcuts.xml` with the variant's applicationId; a variant without resources gets nothing. */
internal fun Project.registerShortcutsTask(variant: ApplicationVariant) {
    val resSource = variant.sources.res ?: return
    val variantName = variant.name[0].uppercaseChar() + variant.name.substring(1)
    val task = tasks.register(
        "replace${variantName}ShortcutPlaceholder",
        ReplaceShortcutsPlaceholderTask::class.java,
    ) {
        applicationId.set(variant.applicationId)
        shortcutsFile.set(projectDir.resolve("src/main/shortcuts.xml"))
    }
    resSource.addGeneratedSourceDirectory(task) { it.outputDir }
}
