package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.viewModelScope
import eu.kanade.domain.manga.interactor.SetMangaViewerFlags
import eu.kanade.domain.manga.model.readerOrientation
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import exh.util.defaultReaderType
import exh.util.mangaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import tachiyomi.core.common.preference.toggle
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.api.get

/**
 * The per-manga viewer settings of the reader: reading mode, orientation and crop-borders, read
 * with the preference fallback and written back to the manga's viewer flags. Composed by
 * [ReaderViewModel].
 */
internal class ReaderViewerSettings(
    private val model: ReaderViewModel,
    private val readerPreferences: ReaderPreferences,
    private val sourceManager: SourceManager,
    private val getManga: GetManga,
    private val setMangaViewerFlags: SetMangaViewerFlags,
) {
    /**
     * Returns the viewer position used by this manga or the default one.
     */
    fun getMangaReadingMode(resolveDefault: Boolean = true): Int {
        val default = readerPreferences.defaultReadingMode.get()
        val manga = model.manga ?: return default
        val readingMode = ReadingMode.fromPreference(manga.readingMode.toInt())
        // SY -->
        return when {
            resolveDefault && readingMode == ReadingMode.DEFAULT && readerPreferences.useAutoWebtoon.get() -> {
                manga.defaultReaderType(manga.mangaType(sourceName = sourceManager.get(manga.source)?.name))
                    ?: default
            }
            resolveDefault && readingMode == ReadingMode.DEFAULT -> {
                default
            }
            else -> {
                manga.readingMode.toInt()
            }
        }
        // SY <--
    }

    /**
     * Updates the viewer position for the open manga.
     */
    fun setMangaReadingMode(readingMode: ReadingMode) {
        val manga = model.manga ?: return
        runBlocking(Dispatchers.IO) {
            setMangaViewerFlags.awaitSetReadingMode(manga.id, readingMode.flagValue.toLong())
            val currChapters = model.state.value.viewerChapters
            if (currChapters != null) {
                // Save current page
                val currChapter = currChapters.currChapter
                currChapter.requestedPage = currChapter.chapter.lastPageRead

                model.mutableState.update {
                    it.copy(
                        manga = getManga.await(manga.id),
                        viewerChapters = currChapters,
                    )
                }
                model.eventChannel.send(Event.ReloadViewerChapters)
            }
        }
    }

    /**
     * Returns the orientation type used by this manga or the default one.
     */
    fun getMangaOrientation(resolveDefault: Boolean = true): Int {
        val default = readerPreferences.defaultOrientationType.get()
        val orientation = ReaderOrientation.fromPreference(model.manga?.readerOrientation?.toInt())
        return if (resolveDefault && orientation == ReaderOrientation.DEFAULT) {
            default
        } else {
            model.manga?.readerOrientation?.toInt() ?: default
        }
    }

    /**
     * Updates the orientation type for the open manga.
     */
    fun setMangaOrientationType(orientation: ReaderOrientation) {
        val manga = model.manga ?: return
        model.viewModelScope.launchIO {
            setMangaViewerFlags.awaitSetOrientation(manga.id, orientation.flagValue.toLong())
            val currChapters = model.state.value.viewerChapters
            if (currChapters != null) {
                // Save current page
                val currChapter = currChapters.currChapter
                currChapter.requestedPage = currChapter.chapter.lastPageRead

                model.mutableState.update {
                    it.copy(
                        manga = getManga.await(manga.id),
                        viewerChapters = currChapters,
                    )
                }
                model.eventChannel.send(Event.SetOrientation(getMangaOrientation()))
                model.eventChannel.send(Event.ReloadViewerChapters)
            }
        }
    }

    // SY -->
    fun toggleCropBorders(): Boolean {
        val readingMode = getMangaReadingMode()
        val isPagerType = ReadingMode.isPagerType(readingMode)
        val isWebtoon = ReadingMode.WEBTOON.flagValue == readingMode
        return if (isPagerType) {
            readerPreferences.cropBorders.toggle()
        } else if (isWebtoon) {
            readerPreferences.cropBordersWebtoon.toggle()
        } else {
            readerPreferences.cropBordersContinuousVertical.toggle()
        }
    }
    // SY <--
}
