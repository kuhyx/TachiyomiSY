package eu.kanade.presentation.more.settings.screen

import android.content.Context
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.SourcePreferences.DataSaver
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
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import okhttp3.Headers
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
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

private fun Context.clearWebViewData() {
    try {
        WebView(this).run {
            setDefaultSettings()
            clearCache(true)
            clearFormData()
            clearHistory()
            clearSslPreferences()
        }
        WebStorage.getInstance().deleteAllData()
        applicationInfo?.dataDir?.let {
            File("$it/app_webview/").deleteRecursively()
        }
        toast(MR.strings.webview_data_deleted)
    } catch (expected: Throwable) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected)
        toast(MR.strings.cache_delete_error)
    }
}

// OkHttp checks for valid values internally; an invalid header value is rejected before it is stored.
private fun Context.acceptUserAgent(value: String): Boolean {
    return try {
        Headers.Builder().add("User-Agent", value)
        toast(MR.strings.requires_app_restart)
        true
    } catch (_: IllegalArgumentException) {
        toast(MR.strings.error_user_agent_string_invalid)
        false
    }
}

@Composable
internal fun SettingsAdvancedScreen.getExtensionsGroup(
    basePreferences: BasePreferences,
): Preference.PreferenceGroup {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val extensionInstallerPref = basePreferences.extensionInstaller
    var shizukuMissing by rememberSaveable { mutableStateOf(false) }
    val trustExtension = remember { Injekt.get<TrustExtension>() }

    if (shizukuMissing) {
        ShizukuMissingDialog { shizukuMissing = false }
    }
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.label_extensions),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = extensionInstallerPref,
                entries = extensionInstallerPref.entries
                    .filter {
                        // Follow-up: allow private option in stable versions once URL handling is more fleshed out
                        // https://github.com/kuhyx/TachiyomiSY/issues/13
                        if (isPreviewBuildType || isDebugBuildType) {
                            true
                        } else {
                            it != BasePreferences.ExtensionInstaller.PRIVATE
                        }
                    }
                    .associateWith { stringResource(it.titleRes) },
                title = stringResource(MR.strings.ext_installer_pref),
                onValueChanged = {
                    if (it == BasePreferences.ExtensionInstaller.SHIZUKU &&
                        !context.isShizukuInstalled
                    ) {
                        shizukuMissing = true
                        false
                    } else {
                        true
                    }
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.ext_revoke_trust),
                onClick = {
                    trustExtension.revokeAll()
                    context.toast(MR.strings.requires_app_restart)
                },
            ),
        ),
    )
}

@Composable
private fun ShizukuMissingDialog(dismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(text = stringResource(MR.strings.ext_installer_shizuku)) },
        text = { Text(text = stringResource(MR.strings.ext_installer_shizuku_unavailable_dialog)) },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dismiss()
                    uriHandler.openUri("https://shizuku.rikka.app/download")
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
internal fun SettingsAdvancedScreen.getDataSaverGroup(): Preference.PreferenceGroup {
    val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
    val dataSaver by sourcePreferences.dataSaver.collectAsState()
    return Preference.PreferenceGroup(
        title = stringResource(SYMR.strings.data_saver),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = sourcePreferences.dataSaver,
                title = stringResource(SYMR.strings.data_saver),
                subtitle = stringResource(SYMR.strings.data_saver_summary),
                entries = mapOf(
                    DataSaver.NONE to stringResource(MR.strings.disabled),
                    DataSaver.BANDWIDTH_HERO to stringResource(SYMR.strings.bandwidth_hero),
                    DataSaver.WSRV_NL to stringResource(SYMR.strings.wsrv),
                ),
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = sourcePreferences.dataSaverServer,
                title = stringResource(SYMR.strings.bandwidth_data_saver_server),
                subtitle = stringResource(SYMR.strings.data_saver_server_summary),
                enabled = dataSaver == DataSaver.BANDWIDTH_HERO,
            ),
        ) + dataSaverImagePreferences(sourcePreferences, dataSaver),
    )
}

// The per-image knobs; all of them only apply while a data saver is on.
@Composable
private fun dataSaverImagePreferences(
    sourcePreferences: SourcePreferences,
    dataSaver: DataSaver,
): List<Preference.PreferenceItem<out Any, out Any>> {
    val enabled = dataSaver != DataSaver.NONE
    val dataSaverImageFormatJpeg by sourcePreferences.dataSaverImageFormatJpeg.collectAsState()
    return listOf(
        Preference.PreferenceItem.SwitchPreference(
            preference = sourcePreferences.dataSaverDownloader,
            title = stringResource(SYMR.strings.data_saver_downloader),
            enabled = enabled,
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = sourcePreferences.dataSaverIgnoreJpeg,
            title = stringResource(SYMR.strings.data_saver_ignore_jpeg),
            enabled = enabled,
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = sourcePreferences.dataSaverIgnoreGif,
            title = stringResource(SYMR.strings.data_saver_ignore_gif),
            enabled = enabled,
        ),
        Preference.PreferenceItem.ListPreference(
            preference = sourcePreferences.dataSaverImageQuality,
            title = stringResource(SYMR.strings.data_saver_image_quality),
            subtitle = stringResource(SYMR.strings.data_saver_image_quality_summary),
            entries =
            listOf("10%", "20%", "40%", "50%", "70%", "80%", "90%", "95%").associateBy { it.trimEnd('%').toInt() },
            enabled = enabled,
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = sourcePreferences.dataSaverImageFormatJpeg,
            title = stringResource(SYMR.strings.data_saver_image_format),
            subtitle = if (dataSaverImageFormatJpeg) {
                stringResource(SYMR.strings.data_saver_image_format_summary_on)
            } else {
                stringResource(SYMR.strings.data_saver_image_format_summary_off)
            },
            enabled = enabled,
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = sourcePreferences.dataSaverColorBW,
            title = stringResource(SYMR.strings.data_saver_color_bw),
            enabled = dataSaver == DataSaver.BANDWIDTH_HERO,
        ),
    )
}
