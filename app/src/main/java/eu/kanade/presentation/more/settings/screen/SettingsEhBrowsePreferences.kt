package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.Preference
import exh.source.ExhPreferences
import exh.uconfig.EhCategory
import exh.uconfig.EhLanguage
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/*
 * The language, front-page category and browsing preferences of the E-Hentai settings screen.
 * Part of [SettingsEhScreen]; same package, so its getPreferences() calls them as before.
 */

internal class LanguageDialogState(preference: String) {
    class RowState(original: ColumnState, translated: ColumnState, rewrite: ColumnState) {
        var original by mutableStateOf(original)
        var translated by mutableStateOf(translated)
        var rewrite by mutableStateOf(rewrite)

        fun toPreference() = "${original.value}*${translated.value}*${rewrite.value}"
    }
    enum class ColumnState(val value: String) {
        Unavailable("false"),
        Enabled("true"),
        Disabled("false"),
    }
    val japanese: RowState
    val english: RowState
    val chinese: RowState
    val dutch: RowState
    val french: RowState
    val german: RowState
    val hungarian: RowState
    val italian: RowState
    val korean: RowState
    val polish: RowState
    val portuguese: RowState
    val russian: RowState
    val spanish: RowState
    val thai: RowState
    val vietnamese: RowState
    val notAvailable: RowState
    val other: RowState

    init {
        val settingsLanguages = preference.split("\n")
        japanese = settingsLanguages[EhLanguage.JAPANESE.ordinal].toRowState(true)
        english = settingsLanguages[EhLanguage.ENGLISH.ordinal].toRowState()
        chinese = settingsLanguages[EhLanguage.CHINESE.ordinal].toRowState()
        dutch = settingsLanguages[EhLanguage.DUTCH.ordinal].toRowState()
        french = settingsLanguages[EhLanguage.FRENCH.ordinal].toRowState()
        german = settingsLanguages[EhLanguage.GERMAN.ordinal].toRowState()
        hungarian = settingsLanguages[EhLanguage.HUNGARIAN.ordinal].toRowState()
        italian = settingsLanguages[EhLanguage.ITALIAN.ordinal].toRowState()
        korean = settingsLanguages[EhLanguage.KOREAN.ordinal].toRowState()
        polish = settingsLanguages[EhLanguage.POLISH.ordinal].toRowState()
        portuguese = settingsLanguages[EhLanguage.PORTUGUESE.ordinal].toRowState()
        russian = settingsLanguages[EhLanguage.RUSSIAN.ordinal].toRowState()
        spanish = settingsLanguages[EhLanguage.SPANISH.ordinal].toRowState()
        thai = settingsLanguages[EhLanguage.THAI.ordinal].toRowState()
        vietnamese = settingsLanguages[EhLanguage.VIETNAMESE.ordinal].toRowState()
        notAvailable = settingsLanguages[EhLanguage.NOT_AVAILABLE.ordinal].toRowState()
        other = settingsLanguages[EhLanguage.OTHER.ordinal].toRowState()
    }

    private fun String.toRowState(disableFirst: Boolean = false) = split("*")
        .map {
            if (it.toBoolean()) {
                ColumnState.Enabled
            } else {
                ColumnState.Disabled
            }
        }
        .let {
            if (disableFirst) {
                RowState(ColumnState.Unavailable, it[1], it[2])
            } else {
                RowState(it[0], it[1], it[2])
            }
        }

    fun toPreference() = listOf(
        japanese,
        english,
        chinese,
        dutch,
        french,
        german,
        hungarian,
        italian,
        korean,
        polish,
        portuguese,
        russian,
        spanish,
        thai,
        vietnamese,
        notAvailable,
        other,
    ).joinToString("\n") { it.toPreference() }
}

@Composable
internal fun LanguageDialogRowCheckbox(
    columnState: LanguageDialogState.ColumnState,
    onStateChange: (LanguageDialogState.ColumnState) -> Unit,
) {
    if (columnState != LanguageDialogState.ColumnState.Unavailable) {
        Checkbox(
            checked = columnState == LanguageDialogState.ColumnState.Enabled,
            onCheckedChange = {
                if (it) {
                    onStateChange(LanguageDialogState.ColumnState.Enabled)
                } else {
                    onStateChange(LanguageDialogState.ColumnState.Disabled)
                }
            },
        )
    } else {
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
internal fun LanguageDialogRow(
    language: String,
    row: LanguageDialogState.RowState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = language,
            modifier = Modifier
                .padding(4.dp)
                .width(80.dp),
            maxLines = 1,
        )
        LanguageDialogRowCheckbox(row.original, onStateChange = { row.original = it })
        LanguageDialogRowCheckbox(row.translated, onStateChange = { row.translated = it })
        LanguageDialogRowCheckbox(row.rewrite, onStateChange = { row.rewrite = it })
    }
}

@Composable
internal fun LanguagesDialog(
    onDismissRequest: () -> Unit,
    initialValue: String,
    onValueChange: (String) -> Unit,
) {
    val state = remember(initialValue) { LanguageDialogState(initialValue) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(SYMR.strings.language_filtering)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(SYMR.strings.language_filtering_summary))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Language", modifier = Modifier.padding(4.dp))
                    Text(text = "Original", modifier = Modifier.padding(4.dp))
                    Text(text = "Translated", modifier = Modifier.padding(4.dp))
                    Text(text = "Rewrite", modifier = Modifier.padding(4.dp))
                }
                LanguageDialogRow(language = "Japanese", row = state.japanese)
                LanguageDialogRow(language = "English", row = state.english)
                LanguageDialogRow(language = "Chinese", row = state.chinese)
                LanguageDialogRow(language = "Dutch", row = state.dutch)
                LanguageDialogRow(language = "French", row = state.french)
                LanguageDialogRow(language = "German", row = state.german)
                LanguageDialogRow(language = "Hungarian", row = state.hungarian)
                LanguageDialogRow(language = "Italian", row = state.italian)
                LanguageDialogRow(language = "Korean", row = state.korean)
                LanguageDialogRow(language = "Polish", row = state.polish)
                LanguageDialogRow(language = "Portuguese", row = state.portuguese)
                LanguageDialogRow(language = "Russian", row = state.russian)
                LanguageDialogRow(language = "Spanish", row = state.spanish)
                LanguageDialogRow(language = "Thai", row = state.thai)
                LanguageDialogRow(language = "Vietnamese", row = state.vietnamese)
                LanguageDialogRow(language = "N/A", row = state.notAvailable)
                LanguageDialogRow(language = "Other", row = state.other)
            }
        },
        confirmButton = {
            TextButton(onClick = { onValueChange(state.toPreference()) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
internal fun settingsLanguages(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.TextPreference {
    val value by exhPreferences.exhSettingsLanguages.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        LanguagesDialog(
            onDismissRequest = { dialogOpen = false },
            initialValue = value,
            onValueChange = {
                dialogOpen = false
                exhPreferences.exhSettingsLanguages.set(it)
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.language_filtering),
        subtitle = stringResource(SYMR.strings.language_filtering_summary),
        onClick = {
            dialogOpen = true
        },
        enabled = exhentaiEnabled,
    )
}

internal class FrontPageCategoriesDialogState(
    preference: String,
) {
    // One flag per EhCategory, in enum order; the preference stores "disabled" so the flags are inverted.
    val enabled: SnapshotStateList<Boolean> = preference.split(",").map { !it.toBoolean() }.toMutableStateList()

    fun toPreference() = enabled.joinToString(separator = ",") { (!it).toString() }
}

private val frontPageCategoryTitles = listOf(
    EhCategory.DOUJINSHI to "Doujinshi",
    EhCategory.MANGA to "Manga",
    EhCategory.ARTIST_CG to "Artist CG",
    EhCategory.GAME_CG to "Game CG",
    EhCategory.WESTERN to "Western",
    EhCategory.NON_H to "Non-H",
    EhCategory.IMAGE_SET to "Image Set",
    EhCategory.COSPLAY to "Cosplay",
    EhCategory.ASIAN_PORN to "Asian Porn",
    EhCategory.MISC to "Misc",
)

@Composable
internal fun FrontPageCategoriesDialogRow(
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onValueChange(!value) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title)
        Switch(checked = value, onCheckedChange = null)
    }
}

@Composable
internal fun FrontPageCategoriesDialog(
    onDismissRequest: () -> Unit,
    initialValue: String,
    onValueChange: (String) -> Unit,
) {
    val state = remember(initialValue) { FrontPageCategoriesDialogState(initialValue) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(SYMR.strings.frong_page_categories)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(SYMR.strings.fromt_page_categories_summary))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Category", modifier = Modifier.padding(4.dp))
                    Text(text = "Enabled", modifier = Modifier.padding(4.dp))
                }
                frontPageCategoryTitles.forEach { (category, title) ->
                    FrontPageCategoriesDialogRow(
                        title = title,
                        value = state.enabled[category.ordinal],
                        onValueChange = { state.enabled[category.ordinal] = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onValueChange(state.toPreference()) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
internal fun enabledCategories(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.TextPreference {
    val value by exhPreferences.exhEnabledCategories.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        FrontPageCategoriesDialog(
            onDismissRequest = { dialogOpen = false },
            initialValue = value,
            onValueChange = {
                dialogOpen = false
                exhPreferences.exhEnabledCategories.set(it)
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.frong_page_categories),
        subtitle = stringResource(SYMR.strings.fromt_page_categories_summary),
        onClick = {
            dialogOpen = true
        },
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun watchedListDefaultState(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.SwitchPreference {
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.exhWatchedListDefaultState,
        title = stringResource(SYMR.strings.watched_list_default),
        subtitle = stringResource(SYMR.strings.watched_list_state_summary),
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun imageQuality(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.ListPreference<String> {
    return Preference.PreferenceItem.ListPreference(
        preference = exhPreferences.imageQuality,
        title = stringResource(SYMR.strings.eh_image_quality_summary),
        subtitle = stringResource(SYMR.strings.eh_image_quality),
        entries = mapOf(
            "auto" to stringResource(SYMR.strings.eh_image_quality_auto),
            "ovrs_2400" to stringResource(SYMR.strings.eh_image_quality_2400),
            "ovrs_1600" to stringResource(SYMR.strings.eh_image_quality_1600),
            "high" to stringResource(SYMR.strings.eh_image_quality_1280),
            "med" to stringResource(SYMR.strings.eh_image_quality_980),
            "low" to stringResource(SYMR.strings.eh_image_quality_780),
        ),
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun enhancedEhentaiView(exhPreferences: ExhPreferences): Preference.PreferenceItem.SwitchPreference {
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.enhancedEHentaiView,
        title = stringResource(SYMR.strings.pref_enhanced_e_hentai_view),
        subtitle = stringResource(SYMR.strings.pref_enhanced_e_hentai_view_summary),
    )
}
