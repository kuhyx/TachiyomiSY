package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get
import java.util.Locale

/**
 * Finds the available extensions in the [api] and updates [availableExtensionMapFlow].
 */
internal suspend fun ExtensionManager.findAvailableExtensions() {
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
internal fun ExtensionManager.enableAdditionalSubLanguages(extensions: List<Extension.Available>) {
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
internal fun ExtensionManager.updateInstalledStatuses(availableExtensions: List<Extension.Available>) {
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
