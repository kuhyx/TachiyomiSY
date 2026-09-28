package eu.kanade.tachiyomi.ui.reader.setting

import cafe.adriel.voyager.core.model.ScreenModel
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ReaderSettingsScreenModel(
    readerState: StateFlow<ReaderViewModel.State>,
    val onChangeReadingMode: (ReadingMode) -> Unit,
    val onChangeOrientation: (ReaderOrientation) -> Unit,
    val preferences: ReaderPreferences = Injekt.get(),
) : ScreenModel {

    // SY -->
    // Its own scope, not Voyager's ioCoroutineScope: the reader builds this model outside any Navigator,
    // so the store keyed that scope by whichever screen model was registered last and never disposed it,
    // and every closed reader stayed reachable through these stateIn coroutines.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // SY <--

    val viewerFlow = readerState
        .map { it.viewer }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Lazily, null)

    val mangaFlow = readerState
        .map { it.manga }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Lazily, null)

    override fun onDispose() {
        scope.cancel()
    }
}
