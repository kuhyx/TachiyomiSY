package eu.kanade.tachiyomi.extension

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.api.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import exh.log.xLogD
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.api.get

internal fun ExtensionManager.getExtensionPackage(sourceId: Long): String? {
    return installedExtensionsFlow.value.find { extension ->
        extension.sources.any { it.id == sourceId }
    }
        ?.pkgName
}

internal fun ExtensionManager.getExtensionPackageAsFlow(sourceId: Long): Flow<String?> {
    return installedExtensionsFlow.map { extensions ->
        extensions.find { extension ->
            extension.sources.any { it.id == sourceId }
        }
            ?.pkgName
    }
}

internal fun ExtensionManager.getAppIconForSource(sourceId: Long): Drawable? {
    val pkgName = getExtensionPackage(sourceId)

    if (pkgName != null) {
        return iconMap[pkgName] ?: iconMap.getOrPut(pkgName) {
            ExtensionLoader.getExtensionPackageInfo(context, pkgName)!!.applicationInfo!!
                .loadIcon(context.packageManager)
        }
    }

    // SY -->
    return when (sourceId) {
        EH_SOURCE_ID -> ContextCompat.getDrawable(context, R.mipmap.ic_ehentai_source)
        EXH_SOURCE_ID -> ContextCompat.getDrawable(context, R.mipmap.ic_exhentai_source)
        MERGED_SOURCE_ID -> ContextCompat.getDrawable(context, R.mipmap.ic_merged_source)
        else -> null
    }
    // SY <--
}

internal fun ExtensionManager.getSourceData(id: Long) = availableExtensionsSourcesData[id]

// Registers the given extension in this and the source managers.
// @param extension The extension to be registered.
internal fun ExtensionManager.registerNewExtension(extension: Extension.Installed) {
    // SY -->
    if (extension.isBlacklisted()) {
        xLogD("Removing blacklisted extension: (name: String, pkgName: %s)!", extension.name, extension.pkgName)
        return
    }
    // SY <--

    installedExtensionMapFlow.value += extension
}

// Registers the given updated extension in this and the source managers previously removing
// the outdated ones.
// @param extension The extension to be registered.
internal fun ExtensionManager.registerUpdatedExtension(extension: Extension.Installed) {
    // SY -->
    if (extension.isBlacklisted()) {
        xLogD("Removing blacklisted extension: (name: %s, pkgName: %s)!", extension.name, extension.pkgName)
        return
    }
    // SY <--

    installedExtensionMapFlow.value += extension
}

// Unregisters the extension in this and the source managers given its package name. Note this
// method is called for every uninstalled application in the system.
// @param pkgName The package name of the uninstalled application.
internal fun ExtensionManager.unregisterExtension(pkgName: String) {
    installedExtensionMapFlow.value -= pkgName
    untrustedExtensionMapFlow.value -= pkgName
}

internal fun ExtensionManager.updatePendingUpdatesCount() {
    val pendingUpdateCount = installedExtensionMapFlow.value.values.count { it.hasUpdate }
    preferences.extensionUpdatesCount.set(pendingUpdateCount)
    if (pendingUpdateCount == 0) {
        ExtensionUpdateNotifier(context).dismiss()
    }
}
