package eu.kanade.presentation.more.settings.screen

import android.content.Context
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_360
import eu.kanade.tachiyomi.network.PREF_DOH_ADGUARD
import eu.kanade.tachiyomi.network.PREF_DOH_ALIDNS
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.network.PREF_DOH_CONTROLD
import eu.kanade.tachiyomi.network.PREF_DOH_DNSPOD
import eu.kanade.tachiyomi.network.PREF_DOH_GOOGLE
import eu.kanade.tachiyomi.network.PREF_DOH_MULLVAD
import eu.kanade.tachiyomi.network.PREF_DOH_NJALLA
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD101
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD9
import eu.kanade.tachiyomi.network.PREF_DOH_SHECAN
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import okhttp3.Headers
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

@Composable
internal fun SettingsAdvancedScreen.getNetworkGroup(
    networkPreferences: NetworkPreferences,
): Preference.PreferenceGroup {
    val context = LocalContext.current
    val networkHelper = remember { Injekt.get<NetworkHelper>() }
    val userAgentPref = networkPreferences.defaultUserAgent
    val userAgent by userAgentPref.collectAsState()

    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.label_network),
        preferenceItems = listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_clear_cookies),
                onClick = {
                    networkHelper.cookieJar.removeAll()
                    context.toast(MR.strings.cookies_cleared)
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_clear_webview_data),
                onClick = { context.clearWebViewData() },
            ),
            Preference.PreferenceItem.ListPreference(
                preference = networkPreferences.dohProvider,
                entries = mapOf(-1 to stringResource(MR.strings.disabled)) + DOH_PROVIDERS,
                title = stringResource(MR.strings.pref_dns_over_https),
                onValueChanged = {
                    context.toast(MR.strings.requires_app_restart)
                    true
                },
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = userAgentPref,
                title = stringResource(MR.strings.pref_user_agent_string),
                onValueChanged = { context.acceptUserAgent(it) },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_reset_user_agent_string),
                enabled = remember(userAgent) { userAgent != userAgentPref.defaultValue() },
                onClick = {
                    userAgentPref.delete()
                    context.toast(MR.strings.requires_app_restart)
                },
            ),
        ),
    )
}

internal fun Context.clearWebViewData() {
    try {
        WebView(this).run {
            setDefaultSettings()
            clearCache(true)
            clearFormData()
            clearHistory()
            clearSslPreferences()
        }
        WebStorage.getInstance().deleteAllData()
        // Android always gives an installed app its info and data directory.
        File("${applicationInfo.dataDir}/app_webview/").deleteRecursively()
        toast(MR.strings.webview_data_deleted)
    } catch (expected: Throwable) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected)
        toast(MR.strings.cache_delete_error)
    }
}

// OkHttp checks for valid values internally; an invalid header value is rejected before it is stored.
internal fun Context.acceptUserAgent(value: String): Boolean {
    return try {
        Headers.Builder().add("User-Agent", value)
        toast(MR.strings.requires_app_restart)
        true
    } catch (_: IllegalArgumentException) {
        toast(MR.strings.error_user_agent_string_invalid)
        false
    }
}

private val DOH_PROVIDERS = mapOf(
    PREF_DOH_CLOUDFLARE to "Cloudflare",
    PREF_DOH_GOOGLE to "Google",
    PREF_DOH_ADGUARD to "AdGuard",
    PREF_DOH_QUAD9 to "Quad9",
    PREF_DOH_ALIDNS to "AliDNS",
    PREF_DOH_DNSPOD to "DNSPod",
    PREF_DOH_360 to "360",
    PREF_DOH_QUAD101 to "Quad 101",
    PREF_DOH_MULLVAD to "Mullvad",
    PREF_DOH_CONTROLD to "Control D",
    PREF_DOH_NJALLA to "Njalla",
    PREF_DOH_SHECAN to "Shecan",
)
