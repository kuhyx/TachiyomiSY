package eu.kanade.presentation.category.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.core.preference.asToggleableState
import eu.kanade.presentation.category.visualName
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ChangeCategoryDialog(
    initialSelection: List<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    if (initialSelection.isEmpty()) {
        NoCategoriesDialog(onDismissRequest, onEditCategories)
        return
    }
    var selection by remember { mutableStateOf(initialSelection) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row {
                tachiyomi.presentation.core.components.material.TextButton(onClick = {
                    onDismissRequest()
                    onEditCategories()
                }) {
                    Text(text = stringResource(MR.strings.action_edit))
                }
                Spacer(modifier = Modifier.weight(1f))
                tachiyomi.presentation.core.components.material.TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                tachiyomi.presentation.core.components.material.TextButton(
                    onClick = {
                        onDismissRequest()
                        onConfirm(selection.includedIds(), selection.excludedIds())
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_move_category))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                selection.forEach { checkbox ->
                    CategoryCheckboxRow(checkbox) { changed ->
                        val index = selection.indexOf(changed)
                        if (index != -1) {
                            val mutableList = selection.toMutableList()
                            mutableList[index] = changed.next()
                            selection = mutableList.toList()
                        }
                    }
                }
            }
        },
    )
}

// Shown when there are no categories yet: the only way forward is to create one.
@Composable
internal fun NoCategoriesDialog(onDismissRequest: () -> Unit, onEditCategories: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            tachiyomi.presentation.core.components.material.TextButton(
                onClick = {
                    onDismissRequest()
                    onEditCategories()
                },
            ) {
                Text(text = stringResource(MR.strings.action_edit_categories))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_move_category))
        },
        text = {
            Text(text = stringResource(MR.strings.information_empty_category_dialog))
        },
    )
}

internal fun List<CheckboxState<Category>>.includedIds(): List<Long> =
    filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }.map { it.value.id }

internal fun List<CheckboxState<Category>>.excludedIds(): List<Long> =
    filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }.map { it.value.id }

@Composable
internal fun CategoryCheckboxRow(checkbox: CheckboxState<Category>, onChange: (CheckboxState<Category>) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(checkbox) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (checkbox) {
            is CheckboxState.TriState -> {
                TriStateCheckbox(
                    state = checkbox.asToggleableState(),
                    onClick = { onChange(checkbox) },
                )
            }
            is CheckboxState.State -> {
                Checkbox(
                    checked = checkbox.isChecked,
                    onCheckedChange = { onChange(checkbox) },
                )
            }
        }
        Text(
            text = checkbox.value.visualName,
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
        )
    }
}
