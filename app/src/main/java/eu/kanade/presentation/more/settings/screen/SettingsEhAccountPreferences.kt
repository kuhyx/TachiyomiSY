package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import exh.source.ExhPreferences
import exh.ui.login.EhLoginActivity
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/*
 * The account, title and tag preferences of the E-Hentai settings screen.
 * Part of [SettingsEhScreen]; same package, so its getPreferences() calls them as before.
 */

@Composable
internal fun getLoginPreference(
    exhPreferences: ExhPreferences,
    openWarnConfigureDialogController: () -> Unit,
): Preference.PreferenceItem.SwitchPreference {
    val activityResultContract =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                // Upload settings
                openWarnConfigureDialogController()
            }
        }
    val context = LocalContext.current
    val value by exhPreferences.enableExhentai.collectAsState()
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.enableExhentai,
        title = stringResource(SYMR.strings.enable_exhentai),
        subtitle = if (!value) {
            stringResource(SYMR.strings.requires_login)
        } else {
            null
        },
        onValueChanged = { newVal ->
            if (!newVal) {
                exhPreferences.enableExhentai.set(false)
                true
            } else {
                activityResultContract.launch(EhLoginActivity.newIntent(context))
                false
            }
        },
    )
}

@Composable
internal fun useHentaiAtHome(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.ListPreference<Int> {
    return Preference.PreferenceItem.ListPreference(
        preference = exhPreferences.useHentaiAtHome,
        title = stringResource(SYMR.strings.use_hentai_at_home),
        subtitle = stringResource(SYMR.strings.use_hentai_at_home_summary),
        entries = mapOf(
            0 to stringResource(SYMR.strings.use_hentai_at_home_option_1),
            1 to stringResource(SYMR.strings.use_hentai_at_home_option_2),
        ),
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun useJapaneseTitle(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.SwitchPreference {
    val value by exhPreferences.useJapaneseTitle.collectAsState()
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.useJapaneseTitle,
        title = stringResource(SYMR.strings.show_japanese_titles),
        subtitle = if (value) {
            stringResource(SYMR.strings.show_japanese_titles_option_1)
        } else {
            stringResource(SYMR.strings.show_japanese_titles_option_2)
        },
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun useOriginalImages(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.SwitchPreference {
    val value by exhPreferences.exhUseOriginalImages.collectAsState()
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.exhUseOriginalImages,
        title = stringResource(SYMR.strings.use_original_images),
        subtitle = if (value) {
            stringResource(SYMR.strings.use_original_images_on)
        } else {
            stringResource(SYMR.strings.use_original_images_off)
        },
        enabled = exhentaiEnabled,
    )
}
