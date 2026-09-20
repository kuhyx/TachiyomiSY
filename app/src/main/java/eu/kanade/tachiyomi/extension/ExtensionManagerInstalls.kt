package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import uy.kohesive.injekt.api.get

/**
 * Returns a flow of the installation process for the given extension. It will complete
 * once the extension is installed or throws an error. The process will be canceled if
 * unsubscribed before its completion.
 *
 * @param extension The extension to be installed.
 */
internal fun ExtensionManager.installExtension(extension: Extension.Available): Flow<InstallStep> =
    installer.downloadAndInstall(extension.apkUrl, extension)

/**
 * Returns a flow of the installation process for the given extension. It will complete
 * once the extension is updated or throws an error. The process will be canceled if
 * unsubscribed before its completion.
 *
 * @param extension The extension to be updated.
 */
internal fun ExtensionManager.updateExtension(extension: Extension.Installed): Flow<InstallStep> {
    val availableExt = availableExtensionMapFlow.value[extension.pkgName] ?: return emptyFlow()
    return installExtension(availableExt)
}

internal fun ExtensionManager.cancelInstallUpdateExtension(extension: Extension) {
    installer.cancelInstall(extension.pkgName)
}

/**
 * Sets to "installing" status of an extension installation.
 *
 * @param downloadId The id of the download.
 */
internal fun ExtensionManager.setInstalling(downloadId: Long) {
    installer.updateInstallStep(downloadId, InstallStep.Installing)
}

internal fun ExtensionManager.updateInstallStep(downloadId: Long, step: InstallStep) {
    installer.updateInstallStep(downloadId, step)
}

/**
 * Uninstalls the extension that matches the given package name.
 *
 * @param extension The extension to uninstall.
 */
internal fun ExtensionManager.uninstallExtension(extension: Extension) {
    installer.uninstallApk(extension.pkgName)
}

/**
 * Adds the given extension to the list of trusted extensions. It also loads in background the
 * now trusted extensions.
 *
 * @param extension the extension to trust
 */
internal suspend fun ExtensionManager.trust(extension: Extension.Untrusted) {
    untrustedExtensionMapFlow.value[extension.pkgName] ?: return

    trustExtension.trust(extension.pkgName, extension.versionCode, extension.signatureHash)

    untrustedExtensionMapFlow.value -= extension.pkgName

    ExtensionLoader.loadExtensionFromPkgName(context, extension.pkgName)
        .let { it as? LoadResult.Success }
        ?.let { registerNewExtension(it.extension) }
}
