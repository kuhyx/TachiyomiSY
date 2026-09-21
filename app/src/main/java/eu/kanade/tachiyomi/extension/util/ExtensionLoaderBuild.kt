package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader.ExtensionHeader
import eu.kanade.tachiyomi.extension.util.ExtensionLoader.ExtensionInfo
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

internal fun ExtensionLoader.extensionHeader(context: Context, pkgInfo: PackageInfo): ExtensionHeader? {
    val appInfo = pkgInfo.applicationInfo!!
    val extName = appInfo.metaData.getString(METADATA_NAME)
        ?: context.packageManager.getApplicationLabel(appInfo).toString().substringAfter("Tachiyomi: ")
    val versionName = pkgInfo.versionName
    // Validate lib version
    val libVersion = versionName?.let { name ->
        appInfo.metaData.getFloat(METADATA_EXTENSION_LIB)
            .takeUnless { it == 0.0f }
            ?.toString()
            ?.toDouble()
            ?: name.substringBeforeLast('.').toDoubleOrNull()
    }
    val isNsfw = appInfo.metaData.getInt(METADATA_CONTENT_WARNING) > 0 ||
        appInfo.metaData.getInt(METADATA_NSFW) == 1
    return when {
        versionName.isNullOrEmpty() -> {
            logcat(LogPriority.WARN) { "Missing versionName for extension $extName" }
            null
        }
        libVersion == null || libVersion !in SUPPORTED_LIB_VERSIONS -> {
            logcat(LogPriority.WARN) {
                "Lib version is $libVersion, while only version(s) " +
                    "${SUPPORTED_LIB_VERSIONS.joinToString()} are supported"
            }
            null
        }
        else -> {
            ExtensionHeader(
                name = extName,
                versionName = versionName,
                versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
                libVersion = libVersion,
                isNsfw = isNsfw,
            )
        }
    }
}

// The load result that stops an unsigned or untrusted package, or null when it may be loaded.
internal suspend fun ExtensionLoader.checkTrust(pkgInfo: PackageInfo, header: ExtensionHeader): LoadResult? {
    val pkgName = pkgInfo.packageName
    val signatures = getSignatures(pkgInfo)
    return when {
        signatures.isNullOrEmpty() -> {
            logcat(LogPriority.WARN) { "Package $pkgName isn't signed" }
            LoadResult.Error
        }
        !trustExtension.isTrusted(pkgInfo, signatures) -> {
            val extension = Extension.Untrusted(
                header.name,
                pkgName,
                header.versionName,
                header.versionCode,
                header.libVersion,
                signatures.last(),
            )
            logcat(LogPriority.WARN) { "Extension $pkgName isn't trusted" }
            LoadResult.Untrusted(extension)
        }
        else -> {
            null
        }
    }
}

internal fun ExtensionLoader.buildExtension(
    context: Context,
    extensionInfo: ExtensionInfo,
    header: ExtensionHeader,
): LoadResult {
    if (!loadNsfwSource && header.isNsfw) return nsfwNotAllowed(extensionInfo.packageInfo.packageName)
    val pkgInfo = extensionInfo.packageInfo
    val appInfo = pkgInfo.applicationInfo!!
    val sources = loadSources(context, pkgInfo, appInfo, header.name) ?: return LoadResult.Error
    val langs = sources.map { it.lang }.toSet()
    val lang = when (langs.size) {
        0 -> ""
        1 -> langs.first()
        else -> "all"
    }
    val extension = Extension.Installed(
        name = header.name,
        pkgName = pkgInfo.packageName,
        versionName = header.versionName,
        versionCode = header.versionCode,
        libVersion = header.libVersion,
        lang = lang,
        isNsfw = header.isNsfw,
        sources = sources,
        pkgFactory = appInfo.metaData.getString(METADATA_SOURCE_FACTORY),
        icon = appInfo.loadIcon(context.packageManager),
        isShared = extensionInfo.isShared,
    )
    return LoadResult.Success(extension)
}

internal fun ExtensionLoader.nsfwNotAllowed(pkgName: String): LoadResult {
    logcat(LogPriority.WARN) { "NSFW extension $pkgName not allowed" }
    return LoadResult.Error
}
