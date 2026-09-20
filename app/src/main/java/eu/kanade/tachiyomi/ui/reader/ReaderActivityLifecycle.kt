package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext

/**
 * Starts the view model from the launch intent's manga/chapter (and SY page) extras.
 * @return false when the extras are missing and the activity should finish.
 */
internal fun ReaderActivity.initViewModelFromIntent(): Boolean {
    val manga = intent.extras?.getLong("manga", -1) ?: -1L
    val chapter = intent.extras?.getLong("chapter", -1) ?: -1L
    // SY -->
    val page = intent.extras?.getInt("page", -1).takeUnless { it == -1 }
    // SY <--
    if (manga == -1L || chapter == -1L) return false
    NotificationReceiver.dismissNotification(this, manga.hashCode(), Notifications.ID_NEW_CHAPTERS)

    lifecycleScope.launchNonCancellable {
        val initResult = viewModel.init(manga, chapter/* SY --> */, page/* SY <-- */)
        if (!initResult.getOrDefault(false)) {
            val exception = initResult.exceptionOrNull() ?: IllegalStateException("Unknown err")
            withUIContext {
                setInitialChapterError(exception)
            }
        }
    }
    return true
}

/** Wires the view model's state and one-shot events to the activity for its lifetime. */
internal fun ReaderActivity.observeViewModel() {
    // Finish when incognito mode is disabled
    preferences.incognitoMode.changes()
        .drop(1)
        .onEach { if (!it) finish() }
        .launchIn(lifecycleScope)

    viewModel.state
        .map { it.isLoadingAdjacentChapter }
        .distinctUntilChanged()
        .onEach(::setProgressDialog)
        .launchIn(lifecycleScope)

    viewModel.state
        .map { it.manga }
        .distinctUntilChanged()
        .filterNotNull()
        .onEach { updateViewer() }
        .launchIn(lifecycleScope)

    viewModel.state
        .map { it.viewerChapters }
        .distinctUntilChanged()
        .filterNotNull()
        .onEach(::setChapters)
        .launchIn(lifecycleScope)

    viewModel.eventFlow
        .onEach(::onViewModelEvent)
        .launchIn(lifecycleScope)
}

private fun ReaderActivity.onViewModelEvent(event: ReaderViewModel.Event) {
    when (event) {
        ReaderViewModel.Event.ReloadViewerChapters -> viewModel.state.value.viewerChapters?.let(::setChapters)
        ReaderViewModel.Event.PageChanged -> displayRefreshHost.flash()
        is ReaderViewModel.Event.SetOrientation -> setOrientation(event.orientation)
        is ReaderViewModel.Event.SavedImage -> onSaveImageResult(event.result)
        is ReaderViewModel.Event.ShareImage ->
            onShareImageResult(event.uri, event.page /* SY --> */, event.secondPage /* SY <-- */)
        is ReaderViewModel.Event.CopyImage -> onCopyImageResult(event.uri)
        is ReaderViewModel.Event.SetCoverResult -> onSetAsCoverResult(event.result)
    }
}
