package eu.kanade.tachiyomi.ui.manga.track

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.track.TrackInfoDialogHome
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.openInBrowser
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class TrackInfoDialogHomeScreen(
    private val mangaId: Long,
    private val mangaTitle: String,
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { TrackInfoDialogHomeModel(mangaId, sourceId) }

        val dateFormat = remember { UiPreferences.dateFormat(Injekt.get<UiPreferences>().dateFormat.get()) }
        val state by screenModel.state.collectAsState()

        // SY -->
        Column(modifier = Modifier.animateContentSize()) {
            if (state.isLoading) {
                LoadingPlaceholder()
            } else {
                // SY <--
                TrackInfoDialogHome(
                    trackItems = state.trackItems,
                    dateFormat = dateFormat,
                    onStatusClick =
                    { navigator.push(TrackStatusSelectorScreen(track = it.track!!, serviceId = it.tracker.id)) },
                    onChapterClick = {
                        navigator.push(TrackChapterSelectorScreen(track = it.track!!, serviceId = it.tracker.id))
                    },
                    onScoreClick =
                    { navigator.push(TrackScoreSelectorScreen(track = it.track!!, serviceId = it.tracker.id)) },
                    onStartDateEdit = { navigator.push(dateSelector(it, start = true)) },
                    onEndDateEdit = { navigator.push(dateSelector(it, start = false)) },
                    onNewSearch = {
                        if (it.tracker is EnhancedTracker) {
                            screenModel.registerEnhancedTracking(it)
                        } else {
                            // SY -->
                            screenModel.newSearch(navigator, it, mangaTitle)
                            // SY <--
                        }
                    },
                    onOpenInBrowser = { openTrackerInBrowser(context, it) },
                    onRemoved = {
                        navigator.push(
                            TrackerRemoveScreen(
                                mangaId =
                                mangaId,
                                track = it.track!!, serviceId = it.tracker.id,
                            ),
                        )
                    },
                    onCopyLink = { context.copyTrackerLink(it) },
                    onTogglePrivate = screenModel::togglePrivate,
                )
            }
        }
    }
}

internal fun TrackInfoDialogHomeScreen.openTrackerInBrowser(context: Context, trackItem: TrackItem) {
    val url = trackItem.track?.remoteUrl ?: return
    if (url.isNotBlank()) {
        context.openInBrowser(url)
    }
}

internal fun Context.copyTrackerLink(trackItem: TrackItem) {
    val url = trackItem.track?.remoteUrl ?: return
    if (url.isNotBlank()) {
        copyToClipboard(url, url)
    }
}

// SY -->
@Composable
internal fun LoadingPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(MR.strings.loading),
            fontSize = 14.sp,
        )
    }
}
