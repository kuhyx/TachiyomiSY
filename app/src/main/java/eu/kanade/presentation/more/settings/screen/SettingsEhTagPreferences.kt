package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import exh.source.ExhPreferences
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun watchedTags(exhentaiEnabled: Boolean): Preference.PreferenceItem.TextPreference {
    val context = LocalContext.current
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.watched_tags),
        subtitle = stringResource(SYMR.strings.watched_tags_summary),
        onClick = {
            context.startActivity(
                WebViewActivity.newIntent(
                    context,
                    url = "https://exhentai.org/mytags",
                    title = context.stringResource(SYMR.strings.watched_tags_exh),
                ),
                null,
            )
        },
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun TagThresholdDialog(
    onDismissRequest: () -> Unit,
    title: String,
    initialValue: Int,
    valueRange: IntRange,
    outsideRangeError: String,
    onValueChange: (Int) -> Unit,
) {
    var value by remember(initialValue) {
        mutableStateOf(initialValue.toString())
    }
    val isValid = remember(value) { value.toIntOrNull().let { it != null && it in valueRange } }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                // Enabled only while the value parses and is in range.
                onClick = { onValueChange(value.toInt()) },
                enabled = isValid,
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    maxLines = 1,
                    singleLine = true,
                    isError = !isValid,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (!isValid) {
                        { Icon(Icons.Outlined.Error, outsideRangeError) }
                    } else {
                        null
                    },
                )
                if (!isValid) {
                    Text(
                        text = outsideRangeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        },
    )
}

@Composable
internal fun tagFilterThreshold(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.TextPreference {
    val value by exhPreferences.ehTagFilterValue.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        TagThresholdDialog(
            onDismissRequest = { dialogOpen = false },
            title = stringResource(SYMR.strings.tag_filtering_threshold),
            initialValue = value,
            valueRange = -9999..0,
            outsideRangeError = stringResource(SYMR.strings.tag_filtering_threshhold_error),
            onValueChange = {
                dialogOpen = false
                exhPreferences.ehTagFilterValue.set(it)
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.tag_filtering_threshold),
        subtitle = stringResource(SYMR.strings.tag_filtering_threshhold_summary, value),
        onClick = {
            dialogOpen = true
        },
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun tagWatchingThreshold(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.TextPreference {
    val value by exhPreferences.ehTagWatchingValue.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        TagThresholdDialog(
            onDismissRequest = { dialogOpen = false },
            title = stringResource(SYMR.strings.tag_watching_threshhold),
            initialValue = value,
            valueRange = 0..9999,
            outsideRangeError = stringResource(SYMR.strings.tag_watching_threshhold_error),
            onValueChange = {
                dialogOpen = false
                exhPreferences.ehTagWatchingValue.set(it)
            },
        )
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.tag_watching_threshhold),
        subtitle = stringResource(SYMR.strings.tag_watching_threshhold_summary, value),
        onClick = {
            dialogOpen = true
        },
        enabled = exhentaiEnabled,
    )
}
