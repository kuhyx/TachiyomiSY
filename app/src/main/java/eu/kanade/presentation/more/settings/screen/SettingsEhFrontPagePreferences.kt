package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.Preference
import exh.source.ExhPreferences
import exh.uconfig.EhCategory
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

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
