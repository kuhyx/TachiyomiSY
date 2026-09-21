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
import androidx.core.text.HtmlCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.source.AndroidSourceManager
import exh.debug.SettingsDebugScreen
import exh.log.EHLogLevel
import exh.pref.DelegateSourcePreferences
import exh.source.BlacklistedSources
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import exh.util.toAnnotatedString
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun SettingsAdvancedScreen.getDeveloperToolsGroup(): Preference.PreferenceGroup {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
    val exhPreferences = remember { Injekt.get<ExhPreferences>() }
    val delegateSourcePreferences = remember { Injekt.get<DelegateSourcePreferences>() }
    val securityPreferences = remember { Injekt.get<SecurityPreferences>() }
    return Preference.PreferenceGroup(
        title = stringResource(SYMR.strings.developer_tools),
        preferenceItems = listOf(
            hentaiFeaturesPreference(exhPreferences),
            Preference.PreferenceItem.SwitchPreference(
                preference = delegateSourcePreferences.delegateSources,
                title = stringResource(SYMR.strings.toggle_delegated_sources),
                subtitle = stringResource(
                    SYMR.strings.toggle_delegated_sources_summary,
                    stringResource(MR.strings.app_name),
                    AndroidSourceManager.DELEGATED_SOURCES.values.map { it.sourceName }.distinct()
                        .joinToString(),
                ),
            ),
            logLevelPreference(exhPreferences),
            Preference.PreferenceItem.SwitchPreference(
                preference = sourcePreferences.enableSourceBlacklist,
                title = stringResource(SYMR.strings.enable_source_blacklist),
                subtitle = stringResource(
                    SYMR.strings.enable_source_blacklist_summary,
                    stringResource(MR.strings.app_name),
                ),
            ),
            encryptDatabasePreference(securityPreferences),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(SYMR.strings.open_debug_menu),
                subtitle = remember {
                    HtmlCompat.fromHtml(
                        context.stringResource(SYMR.strings.open_debug_menu_summary),
                        HtmlCompat.FROM_HTML_MODE_COMPACT,
                    ).toAnnotatedString()
                },
                onClick = { navigator.push(SettingsDebugScreen()) },
            ),
        ),
    )
}

// Enabling the E-Hentai features also unhides both E-H sources; disabling hides them again.
@Composable
internal fun hentaiFeaturesPreference(exhPreferences: ExhPreferences) = Preference.PreferenceItem.SwitchPreference(
    preference = exhPreferences.isHentaiEnabled,
    title = stringResource(SYMR.strings.toggle_hentai_features),
    subtitle = stringResource(SYMR.strings.toggle_hentai_features_summary),
    onValueChanged = {
        if (it) {
            BlacklistedSources.HIDDEN_SOURCES += EH_SOURCE_ID
            BlacklistedSources.HIDDEN_SOURCES += EXH_SOURCE_ID
        } else {
            BlacklistedSources.HIDDEN_SOURCES -= EH_SOURCE_ID
            BlacklistedSources.HIDDEN_SOURCES -= EXH_SOURCE_ID
        }
        true
    },
)

@Composable
internal fun logLevelPreference(exhPreferences: ExhPreferences): Preference.PreferenceItem.ListPreference<Int> {
    val context = LocalContext.current
    return Preference.PreferenceItem.ListPreference(
        preference = exhPreferences.logLevel,
        title = stringResource(SYMR.strings.log_level),
        subtitle = stringResource(SYMR.strings.log_level_summary),
        entries = EHLogLevel.entries.mapIndexed { index, ehLogLevel ->
            index to "${context.stringResource(ehLogLevel.nameRes)} (${
                context.stringResource(ehLogLevel.description)
            })"
        }.toMap(),
    )
}

// Turning encryption on asks for confirmation first; the switch only flips once the dialog is accepted.
@Composable
internal fun encryptDatabasePreference(
    securityPreferences: SecurityPreferences,
): Preference.PreferenceItem<out Any, out Any> {
    val context = LocalContext.current
    var enableEncryptDatabase by rememberSaveable { mutableStateOf(false) }
    if (enableEncryptDatabase) {
        val dismiss = { enableEncryptDatabase = false }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(text = stringResource(SYMR.strings.encrypt_database)) },
            text = {
                Text(
                    text = remember {
                        HtmlCompat.fromHtml(
                            context.stringResource(SYMR.strings.encrypt_database_message),
                            HtmlCompat.FROM_HTML_MODE_COMPACT,
                        ).toAnnotatedString()
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dismiss()
                        securityPreferences.encryptDatabase.set(true)
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
        )
    }
    return Preference.PreferenceItem.SwitchPreference(
        title = stringResource(SYMR.strings.encrypt_database),
        preference = securityPreferences.encryptDatabase,
        subtitle = stringResource(SYMR.strings.encrypt_database_subtitle),
        onValueChanged = {
            if (it) {
                enableEncryptDatabase = true
                false
            } else {
                true
            }
        },
    )
}
