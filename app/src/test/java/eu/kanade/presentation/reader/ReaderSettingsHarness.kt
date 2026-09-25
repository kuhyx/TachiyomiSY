package eu.kanade.presentation.reader

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import kotlinx.coroutines.flow.MutableStateFlow
import tachiyomi.domain.manga.model.Manga

/** A real settings model over in-memory preferences; the reader state holds [manga] and [viewer]. */
internal class ReaderSettingsHarness(manga: Manga? = null, viewer: Viewer? = null) {
    val preferences: ReaderPreferences = ReaderPreferences(FlowPreferenceStore())
    val modes: MutableList<ReadingMode> = mutableListOf()
    val orientations: MutableList<ReaderOrientation> = mutableListOf()
    val state: MutableStateFlow<ReaderViewModel.State> =
        MutableStateFlow(ReaderViewModel.State(manga = manga, viewer = viewer))
    val model: ReaderSettingsScreenModel = ReaderSettingsScreenModel(
        readerState = state,
        onChangeReadingMode = { modes += it },
        onChangeOrientation = { orientations += it },
        preferences = preferences,
    )
}

/** A manga read left to right with a free orientation. */
internal fun readerManga(): Manga = Manga.create().copy(id = 1L, viewerFlags = 0x9L)
