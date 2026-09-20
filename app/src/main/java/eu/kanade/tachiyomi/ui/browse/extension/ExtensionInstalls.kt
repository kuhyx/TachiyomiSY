package eu.kanade.tachiyomi.ui.browse.extension

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import uy.kohesive.injekt.api.get

internal fun ExtensionsScreenModel.updateAllExtensions() {
    screenModelScope.launchIO {
        state.value.items.values.flatten()
            .map { it.extension }
            .filterIsInstance<Extension.Installed>()
            .filter { it.hasUpdate }
            .forEach(::updateExtension)
    }
}

internal fun ExtensionsScreenModel.installExtension(extension: Extension.Available) {
    screenModelScope.launchIO {
        extensionManager.installExtension(extension).collectToInstallUpdate(extension)
    }
}

internal fun ExtensionsScreenModel.updateExtension(extension: Extension.Installed) {
    screenModelScope.launchIO {
        extensionManager.updateExtension(extension).collectToInstallUpdate(extension)
    }
}

internal fun ExtensionsScreenModel.cancelInstallUpdateExtension(extension: Extension) {
    extensionManager.cancelInstallUpdateExtension(extension)
    removeDownloadState(extension)
}

internal fun ExtensionsScreenModel.uninstallExtension(extension: Extension) {
    extensionManager.uninstallExtension(extension)
}

internal fun ExtensionsScreenModel.trustExtension(extension: Extension.Untrusted) {
    screenModelScope.launch {
        extensionManager.trust(extension)
    }
}
