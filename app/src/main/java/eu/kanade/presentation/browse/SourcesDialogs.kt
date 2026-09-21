package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.contains
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.isLocal

@Composable
internal fun SourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    // SY -->
    onClickSetCategories: (() -> Unit)?,
    onClickToggleDataSaver: (() -> Unit)?,
    // SY <--
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            SourceOptions(
                source = source,
                onClickPin = onClickPin,
                onClickDisable = onClickDisable,
                onClickSetCategories = onClickSetCategories,
                onClickToggleDataSaver = onClickToggleDataSaver,
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}

@Composable
internal fun SourceOptions(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    onClickSetCategories: (() -> Unit)?,
    onClickToggleDataSaver: (() -> Unit)?,
) {
    Column {
        val textId = if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin
        SourceOption(text = stringResource(textId), onClick = onClickPin)
        if (!source.isLocal()) {
            SourceOption(text = stringResource(MR.strings.action_disable), onClick = onClickDisable)
        }
        // SY -->
        if (onClickSetCategories != null) {
            SourceOption(text = stringResource(MR.strings.categories), onClick = onClickSetCategories)
        }
        if (onClickToggleDataSaver != null) {
            val dataSaverId = if (source.isExcludedFromDataSaver) {
                SYMR.strings.data_saver_stop_exclude
            } else {
                SYMR.strings.data_saver_exclude
            }
            SourceOption(text = stringResource(dataSaverId), onClick = onClickToggleDataSaver)
        }
        // SY <--
    }
}

@Composable
internal fun SourceOption(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

// SY -->
@Composable
internal fun SourceCategoriesDialog(
    source: Source,
    categories: List<String>,
    onClickCategories: (List<String>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val newCategories = remember(source) {
        mutableStateListOf<String>().also { it += source.categories }
    }
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                categories.forEach { category ->
                    LabeledCheckbox(
                        label = category,
                        checked = category in newCategories,
                        onCheckedChange = {
                            if (it) {
                                newCategories += category
                            } else {
                                newCategories -= category
                            }
                        },
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onClickCategories(newCategories.toList()) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}
