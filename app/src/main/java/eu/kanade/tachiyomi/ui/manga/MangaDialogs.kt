package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.Dialog
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.dismissDialog() {
    updateSuccessState { it.copy(dialog = null) }
}

internal fun MangaScreenModel.showDeleteChapterDialog(chapters: List<Chapter>) {
    updateSuccessState { it.copy(dialog = Dialog.DeleteChapters(chapters)) }
}

internal fun MangaScreenModel.showSettingsDialog() {
    updateSuccessState { it.copy(dialog = Dialog.SettingsSheet) }
}

internal fun MangaScreenModel.showTrackDialog() {
    updateSuccessState { it.copy(dialog = Dialog.TrackSheet) }
}

internal fun MangaScreenModel.showCoverDialog() {
    updateSuccessState { it.copy(dialog = Dialog.FullCover) }
}

internal fun MangaScreenModel.showMigrateDialog(duplicate: Manga) {
    val manga = successState?.manga ?: return
    updateSuccessState { it.copy(dialog = Dialog.Migrate(target = manga, current = duplicate)) }
}

// SY -->
internal fun MangaScreenModel.showEditMangaInfoDialog() {
    updateSuccessState { it.copy(dialog = Dialog.EditMangaInfo(it.manga)) }
}

internal fun MangaScreenModel.showEditMergedSettingsDialog() {
    updateSuccessState { state ->
        state.mergedData?.let { state.copy(dialog = Dialog.EditMergedSettings(it)) } ?: state
    }
}
