package eu.kanade.tachiyomi.ui.manga.merged

import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import eu.kanade.tachiyomi.databinding.EditMergedSettingsDialogBinding
import eu.kanade.tachiyomi.ui.manga.MergedMangaData
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun EditMergedSettingsDialog(
    onDismissRequest: () -> Unit,
    mergedData: MergedMangaData,
    onDeleteClick: (MergedMangaReference) -> Unit,
    onPositiveClick: (List<MergedMangaReference>) -> Unit,
) {
    val context = LocalContext.current
    val state = remember {
        EditMergedSettingsState(context, onDeleteClick, onDismissRequest, onPositiveClick)
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = state::onPositiveButtonClick) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                AndroidView(
                    factory = { factoryContext ->
                        val binding = EditMergedSettingsDialogBinding.inflate(LayoutInflater.from(factoryContext))
                        state.onViewCreated(factoryContext, binding, mergedData.manga.values.toList(), mergedData.references)
                        binding.root
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
    )
}
