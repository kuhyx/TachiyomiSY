package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.util.system.ChildFirstPathClassLoader
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"

/** Instantiates every source class the extension manifest lists, or null (logged) when any of them fails. */
internal fun ExtensionLoader.loadSources(
    context: Context,
    pkgInfo: PackageInfo,
    appInfo: ApplicationInfo,
    extName: String,
): List<Source>? {
    val classLoader = createClassLoader(context, appInfo, extName) ?: return null
    val classNames = appInfo.metaData.getString(METADATA_SOURCE_CLASS)!!
        .split(";")
        .map {
            val sourceClass = it.trim()
            if (sourceClass.startsWith(".")) pkgInfo.packageName + sourceClass else sourceClass
        }
    return classNames.map { instantiateSources(classLoader, it, extName) ?: return null }.flatten()
}

private fun ExtensionLoader.createClassLoader(
    context: Context,
    appInfo: ApplicationInfo,
    extName: String,
): ClassLoader? {
    return try {
        ChildFirstPathClassLoader(appInfo.sourceDir, null, context.classLoader)
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Extension load error: $extName (${appInfo.packageName})" }
        null
    }
}

private fun ExtensionLoader.instantiateSources(
    classLoader: ClassLoader,
    className: String,
    extName: String,
): List<Source>? {
    return try {
        when (val obj = Class.forName(className, false, classLoader).getDeclaredConstructor().newInstance()) {
            is Source -> listOf(obj)
            is SourceFactory -> obj.createSources()
            else -> error("Unknown source class type: ${obj.javaClass}")
        }
    } catch (expected: Throwable) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Extension load error: $extName ($className)" }
        null
    }
}
