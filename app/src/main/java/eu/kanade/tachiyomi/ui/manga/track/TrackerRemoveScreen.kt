package eu.kanade.tachiyomi.ui.manga.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.track.DeletableTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.AlertDialogContent
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class TrackerRemoveScreen(
    private val mangaId: Long,
    private val track: Track,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                mangaId = mangaId,
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }
        val serviceName = screenModel.getName()
        var removeRemoteTrack by remember { mutableStateOf(false) }
        AlertDialogContent(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    text = stringResource(MR.strings.track_delete_title, serviceName),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                RemoveTrackText(
                    serviceName = serviceName,
                    isDeletable = screenModel.isDeletable(),
                    removeRemoteTrack = removeRemoteTrack,
                    onRemoveRemoteTrackChange = { removeRemoteTrack = it },
                )
            },
            buttons = {
                RemoveTrackButtons(
                    onCancel = navigator::pop,
                    onConfirm = {
                        screenModel.unregisterTracking(serviceId)
                        if (removeRemoteTrack) screenModel.deleteMangaFromService()
                        navigator.pop()
                    },
                )
            },
        )
    }

    private class Model(
        private val mangaId: Long,
        private val track: Track,
        private val tracker: Tracker,
        private val deleteTrack: DeleteTrack = Injekt.get(),
    ) : ScreenModel {

        fun getName() = tracker.name

        fun isDeletable() = tracker is DeletableTracker

        fun deleteMangaFromService() {
            screenModelScope.launchNonCancellable {
                try {
                    (tracker as DeletableTracker).delete(track)
                } catch (expected: Exception) {
                    // Logged whatever the cause; the caller carries on.
                    logcat(LogPriority.ERROR, expected) { "Failed to delete entry from service" }
                }
            }
        }

        fun unregisterTracking(serviceId: Long) {
            screenModelScope.launchNonCancellable { deleteTrack.await(mangaId, serviceId) }
        }
    }
}

@Composable
internal fun RemoveTrackText(
    serviceName: String,
    isDeletable: Boolean,
    removeRemoteTrack: Boolean,
    onRemoveRemoteTrackChange: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Text(
            text = stringResource(MR.strings.track_delete_text, serviceName),
        )
        if (isDeletable) {
            LabeledCheckbox(
                label = stringResource(MR.strings.track_delete_remote_text, serviceName),
                checked = removeRemoteTrack,
                onCheckedChange = onRemoveRemoteTrackChange,
            )
        }
    }
}

@Composable
internal fun RemoveTrackButtons(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small, Alignment.End),
    ) {
        TextButton(onClick = onCancel) {
            Text(text = stringResource(MR.strings.action_cancel))
        }
        FilledTonalButton(
            onClick = onConfirm,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Text(text = stringResource(MR.strings.action_ok))
        }
    }
}
