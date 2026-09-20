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
import eu.kanade.tachiyomi.util.system.toast
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
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

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
    private val api = ExtensionApi()

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

    private var subLanguagesEnabledOnFirstRun = preferences.enabledLanguages.isSet()

    internal var availableExtensionsSourcesData: Map<Long, StubSource> = emptyMap()

    private fun setupAvailableSourcesMap(extensions: List<Extension.Available>) {
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
     * Finds the available extensions in the [api] and updates [availableExtensionMapFlow].
     */
    suspend fun findAvailableExtensions() {
        val extensions: List<Extension.Available> = try {
            api.findExtensions()
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
            withUIContext { context.toast(MR.strings.extension_api_error) }
            return
        }

        enableAdditionalSubLanguages(extensions)

        availableExtensionMapFlow.value = extensions.associateBy { it.pkgName }
        updateInstalledStatuses(extensions)
        setupAvailableSourcesMap(extensions)
    }

    // Enables the additional sub-languages in the app first run. This addresses
    // the issue where users still need to enable some specific languages even when
    // the device language is inside that major group. As an example, if a user
    // has a zh device language, the app will also enable zh-Hans and zh-Hant.
    // If the user have already changed the enabledLanguages preference value once,
    // the new languages will not be added to respect the user enabled choices.
    private fun enableAdditionalSubLanguages(extensions: List<Extension.Available>) {
        if (subLanguagesEnabledOnFirstRun || extensions.isEmpty()) {
            return
        }

        // Use the source lang as some aren't present on the extension level.
        val availableLanguages = extensions
            .flatMap(Extension.Available::sources)
            .distinctBy(Extension.Available.Source::lang)
            .map(Extension.Available.Source::lang)

        val deviceLanguage = Locale.getDefault().language
        val defaultLanguages = preferences.enabledLanguages.defaultValue()
        val languagesToEnable = availableLanguages.filter {
            it != deviceLanguage && it.startsWith(deviceLanguage)
        }

        preferences.enabledLanguages.set(defaultLanguages + languagesToEnable)
        subLanguagesEnabledOnFirstRun = true
    }

    // Sets the update field of the installed extensions with the given [availableExtensions].
    // @param availableExtensions The list of extensions given by the [api].
    private fun updateInstalledStatuses(availableExtensions: List<Extension.Available>) {
        if (availableExtensions.isEmpty()) {
            preferences.extensionUpdatesCount.set(0)
            return
        }

        val installedExtensionsMap = installedExtensionMapFlow.value.toMutableMap()
        var changed = false
        for ((pkgName, extension) in installedExtensionsMap) {
            val availableExt = availableExtensions.find { it.pkgName == pkgName }

            if (availableExt == null && !extension.isObsolete) {
                installedExtensionsMap[pkgName] = extension.copy(isObsolete = true)
                changed = true
                // SY -->
            } else if (extension.isBlacklisted() && !extension.isRedundant) {
                installedExtensionsMap[pkgName] = extension.copy(isRedundant = true)
                changed = true
                // SY <--
            } else if (availableExt != null) {
                val hasUpdate = extension.updateExists(availableExt)
                if (extension.hasUpdate != hasUpdate) {
                    installedExtensionsMap[pkgName] = extension.copy(
                        hasUpdate = hasUpdate,
                        store = availableExt.store,
                    )
                } else {
                    installedExtensionsMap[pkgName] = extension.copy(
                        store = availableExt.store,
                    )
                }
                changed = true
            }
        }
        if (changed) {
            installedExtensionMapFlow.value = installedExtensionsMap
        }
        updatePendingUpdatesCount()
    }

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

    private fun Extension.Installed.updateExists(availableExtension: Extension.Available? = null): Boolean {
        val availableExt = availableExtension
            ?: availableExtensionMapFlow.value[pkgName]
            ?: return false

        return availableExt.versionCode > versionCode || availableExt.libVersion > libVersion
    }

    internal operator fun <T : Extension> Map<String, T>.plus(extension: T) = plus(extension.pkgName to extension)

    private fun <T : Extension> StateFlow<Map<String, T>>.mapExtensions(scope: CoroutineScope): StateFlow<List<T>> =
        map { it.values.toList() }.stateIn(scope, SharingStarted.Lazily, value.values.toList())
}
