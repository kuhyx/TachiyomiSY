package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import exh.favorites.FavoritesSyncStatus
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun SyncFavoritesProgressDialog(
    status: FavoritesSyncStatus,
    setStatusIdle: () -> Unit,
    openManga: (Long) -> Unit,
) {
    val context = LocalContext.current
    val properties by produceState<SyncFavoritesProgressProperties?>(initialValue = null, status) {
        val base = context.syncProperties(status, setStatusIdle, openManga)
        value = base
        val slowTitle = (status as? FavoritesSyncStatus.Processing)?.slowGalleryTitle()
        if (base != null && slowTitle != null) {
            delay(5.seconds)
            value = base.copy(text = base.text + "\n\n" + slowTitle)
        }
    }
    val dialog = properties
    if (dialog != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                dialog.positiveButton?.let { button ->
                    TextButton(onClick = button.onClick) {
                        Text(text = button.text)
                    }
                }
            },
            dismissButton = {
                dialog.negativeButton?.let { button ->
                    TextButton(onClick = button.onClick) {
                        Text(text = button.text)
                    }
                }
            },
            title = {
                Text(text = dialog.title)
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(text = dialog.text)
                }
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
            ),
        )
    }
}
