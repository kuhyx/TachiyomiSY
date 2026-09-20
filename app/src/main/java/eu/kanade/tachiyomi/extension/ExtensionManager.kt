package eu.kanade.tachiyomi.extension

import android.content.Context
import android.graphics.drawable.Drawable
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.api.ExtensionApi
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import exh.log.xLogD
import exh.source.BlacklistedSources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.domain.source.model.StubSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The manager of extensions installed as another apk which extend the available sources. It handles
 * the retrieval of remotely available extensions as well as installing, updating and removing them.
 * To avoid malicious distribution, every extension must be signed and it will only be loaded if its
 * signature is trusted, otherwise the user will be prompted with a warning to trust it before being
 * loaded.
 */
internal class ExtensionManager(
    internal val context: Context,
    internal val preferences: SourcePreferences = Injekt.get(),
    internal val trustExtension: TrustExtension = Injekt.get(),
) {

    val scope = CoroutineScope(SupervisorJob())

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // API where all the available extensions can be found.
    internal val api = ExtensionApi()

    // The installer which installs, updates and uninstalls the extensions.
    internal val installer by lazy { ExtensionInstaller(context) }

    internal val iconMap = mutableMapOf<String, Drawable>()

    internal val installedExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.Installed>())
    val installedExtensionsFlow = installedExtensionMapFlow.mapExtensions(scope)

    internal val availableExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.Available>())

    // SY -->
    val availableExtensionsFlow = availableExtensionMapFlow.map { it.filterNotBlacklisted().values.toList() }
        .stateIn(scope, SharingStarted.Lazily, availableExtensionMapFlow.value.values.toList())
    // SY <--

    internal val untrustedExtensionMapFlow = MutableStateFlow(emptyMap<String, Extension.Untrusted>())
    val untrustedExtensionsFlow = untrustedExtensionMapFlow.mapExtensions(scope)

    init {
        initExtensions()
        ExtensionInstallReceiver(InstallationListener()).register(context)
    }

    internal var subLanguagesEnabledOnFirstRun = preferences.enabledLanguages.isSet()

    internal var availableExtensionsSourcesData: Map<Long, StubSource> = emptyMap()

    internal fun setupAvailableSourcesMap(extensions: List<Extension.Available>) {
        if (extensions.isEmpty()) return
        availableExtensionsSourcesData = extensions
            .flatMap { ext -> ext.sources.map { it.toStubSource() } }
            .associateBy { it.id }
    }

    // Loads and registers the installed extensions.
    private fun initExtensions() {
        val extensions = ExtensionLoader.loadExtensions(context)

        installedExtensionMapFlow.value = extensions
            .filterIsInstance<LoadResult.Success>()
            .associate { it.extension.pkgName to it.extension }

        untrustedExtensionMapFlow.value = extensions
            .filterIsInstance<LoadResult.Untrusted>()
            .associate { it.extension.pkgName to it.extension }
            // SY -->
            .filterNotBlacklisted()
        // SY <--

        _isInitialized.value = true
    }

    // EXH -->
    private fun <T : Extension> Map<String, T>.filterNotBlacklisted(): Map<String, T> {
        val blacklistEnabled = preferences.enableSourceBlacklist.get()
        return filterNot { (_, extension) ->
            extension.isBlacklisted(blacklistEnabled)
                .also {
                    if (it) {
                        this@ExtensionManager.xLogD(
                            "Removing blacklisted extension: (name: %s, pkgName: %s)!",
                            extension.name,
                            extension.pkgName,
                        )
                    }
                }
        }
    }

    internal fun Extension.isBlacklisted(blacklistEnabled: Boolean = preferences.enableSourceBlacklist.get()): Boolean =
        pkgName in BlacklistedSources.BLACKLISTED_EXTENSIONS && blacklistEnabled
    // EXH <--

    /**
     * Listener which receives events of the extensions being installed, updated or removed.
     */
    private inner class InstallationListener : ExtensionInstallReceiver.Listener {

        override fun onExtensionInstalled(extension: Extension.Installed) {
            registerNewExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUpdated(extension: Extension.Installed) {
            registerUpdatedExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUntrusted(extension: Extension.Untrusted) {
            installedExtensionMapFlow.value -= extension.pkgName
            untrustedExtensionMapFlow.value += extension
            updatePendingUpdatesCount()
        }

        override fun onPackageUninstalled(pkgName: String) {
            ExtensionLoader.uninstallPrivateExtension(context, pkgName)
            unregisterExtension(pkgName)
            updatePendingUpdatesCount()
        }
    }

    // Extension method to set the update field of an installed extension.
    internal fun Extension.Installed.withUpdateCheck(): Extension.Installed {
        return if (updateExists()) {
            copy(hasUpdate = true)
        } else {
            this
        }
    }

    internal fun Extension.Installed.updateExists(availableExtension: Extension.Available? = null): Boolean {
        val availableExt = availableExtension
            ?: availableExtensionMapFlow.value[pkgName]
            ?: return false

        return availableExt.versionCode > versionCode || availableExt.libVersion > libVersion
    }

    internal operator fun <T : Extension> Map<String, T>.plus(extension: T) = plus(extension.pkgName to extension)

    private fun <T : Extension> StateFlow<Map<String, T>>.mapExtensions(scope: CoroutineScope): StateFlow<List<T>> =
        map { it.values.toList() }.stateIn(scope, SharingStarted.Lazily, value.values.toList())
}
