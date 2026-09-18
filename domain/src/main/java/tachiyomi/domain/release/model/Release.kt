package tachiyomi.domain.release.model

import android.os.Build

/**
 * The latest release as reported by GitHub.
 *
 * @property version Tag of the release.
 * @property info Release notes.
 * @property releaseLink Web page of the release.
 * @property assets Download urls of every attached file.
 */
public data class Release(
    val version: String,
    val info: String,
    val releaseLink: String,
    val assets: List<String>,
)

/** The APK asset matching this device's primary ABI, or the first asset when none matches. */
public fun Release.getDownloadLink(): String {
    val apkVariant = when (Build.SUPPORTED_ABIS[0]) {
        "arm64-v8a" -> "-arm64-v8a"
        "armeabi-v7a" -> "-armeabi-v7a"
        "x86" -> "-x86"
        "x86_64" -> "-x86_64"
        else -> ""
    }

    // SY -->
    return assets.find { it.contains("TachiyomiSY$apkVariant-") } ?: assets[0]
    // SY <--
}
