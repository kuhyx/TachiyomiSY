package eu.kanade.presentation.more.settings.screen

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
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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
