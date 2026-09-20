package mihon.gradle.configurations

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File
import java.util.Properties

/** `-PsyReplaceUpstream`: build `foss` as a drop-in for the official APK instead of a companion. */
public const val SY_REPLACE_UPSTREAM: String = "syReplaceUpstream"

/** `-PsyBuildNumber=<n>`: CI run number appended to the drop-in's version name (`-kuhy.<n>`). */
public const val SY_BUILD_NUMBER: String = "syBuildNumber"

/** `-PsyKeyProperties=<file>`: where the release key material is read from (tests point it at a fixture). */
public const val SY_KEY_PROPERTIES: String = "syKeyProperties"

/** Name of the signing config built from the key material. */
public const val SY_SIGNING_CONFIG: String = "kuhy"

/** Upstream's build type for the F-Droid-style build; the drop-in is this type under [SY_REPLACE_UPSTREAM]. */
public const val SY_BUILD_TYPE: String = "foss"

private const val VERSION_NAME_SUFFIX = "-kuhy"
private const val APPLICATION_ID_SUFFIX = ".foss"
private const val DEFAULT_KEY_PROPERTIES = ".android/release/key.properties"

/**
 * The kuhy fork's release build: a `kuhy` signing config from `~/.android/release/key.properties`
 * (absent on any other machine, in which case no config is created and every stock task keeps
 * working) and the `foss` build type on top of upstream's `release`.
 *
 * Without [SY_REPLACE_UPSTREAM] `foss` is upstream's: a companion app under `.foss`. With it, the
 * build is a replacement for the official APK -- same applicationId, signed with the personal
 * release key so it can be upgraded in place afterwards (the stock app must be uninstalled once
 * first: its signature differs) -- and the version name carries the CI run number when
 * [SY_BUILD_NUMBER] is given, so `1.13.2-kuhy.42` names the workflow run that built it.
 *
 * `foss` is created in `finalizeDsl` because `initWith(release)` copies `release` as configured by
 * the module's own build script, which runs after this plugin is applied.
 */
public fun Project.configureSyRelease() {
    val keys = readKeyProperties(keyPropertiesFile())
    extensions.configure<ApplicationExtension> {
        if (keys.isNotEmpty()) {
            signingConfigs.create(SY_SIGNING_CONFIG) {
                storeFile = file(keys.getProperty("storeFile"))
                storePassword = keys.getProperty("storePassword")
                keyAlias = keys.getProperty("keyAlias")
                keyPassword = keys.getProperty("keyPassword")
            }
        }
    }
    extensions.configure<ApplicationAndroidComponentsExtension> {
        finalizeDsl { android ->
            android.buildTypes.create(SY_BUILD_TYPE) {
                initWith(android.buildTypes.getByName("release"))
                matchingFallbacks.add("release")
                buildConfigField("boolean", "INCLUDE_UPDATER", "false")
                if (hasProperty(SY_REPLACE_UPSTREAM)) {
                    replaceUpstream(this, android, signed = keys.isNotEmpty())
                } else {
                    applicationIdSuffix = APPLICATION_ID_SUFFIX
                }
            }
        }
    }
}

private fun Project.replaceUpstream(foss: ApplicationBuildType, android: ApplicationExtension, signed: Boolean) {
    if (!signed) {
        throw GradleException(
            "-P$SY_REPLACE_UPSTREAM needs the release key: ${keyPropertiesFile()} is missing or empty",
        )
    }
    val buildNumber = findProperty(SY_BUILD_NUMBER)?.run { toString().takeIf { it.isNotBlank() } }
    foss.versionNameSuffix = buildNumber?.let { "$VERSION_NAME_SUFFIX.$it" } ?: VERSION_NAME_SUFFIX
    foss.signingConfig = android.signingConfigs.getByName(SY_SIGNING_CONFIG)
}

private fun Project.keyPropertiesFile(): File {
    val override = findProperty(SY_KEY_PROPERTIES)?.toString()
    return if (override != null) file(override) else File(System.getProperty("user.home"), DEFAULT_KEY_PROPERTIES)
}

private fun readKeyProperties(file: File): Properties = Properties().apply {
    if (file.exists()) file.inputStream().use(::load)
}
