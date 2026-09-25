package eu.kanade.tachiyomi.data.download.model

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.milliseconds

/**
 * One queued chapter download. A mutable state holder (the downloader advances [status] and
 * [pages] in place), so not a data class; equality is still by chapter, as the queue expects.
 */
internal class Download(
    val source: HttpSource,
    val manga: Manga,
    val chapter: Chapter,
) {
    var pages: List<Page>? = null

    val totalProgress: Int
        get() = pages?.sumOf(Page::progress) ?: 0

    val downloadedImages: Int
        get() = pages?.count { it.status == Page.State.Ready } ?: 0

    @Transient
    private val _statusFlow = MutableStateFlow(State.NOT_DOWNLOADED)

    @Transient
    val statusFlow = _statusFlow.asStateFlow()
    val status: State
        get() = _statusFlow.value

    // 0 until the page list is known, then the average of the pages' progress.
    @Transient
    val progressFlow = flow {
        if (pages == null) {
            emit(null)
            while (pages == null) {
                delay(50.milliseconds)
            }
        }
        emit(pages!!)
    }
        .flatMapLatest { pages ->
            if (pages == null) flowOf(0) else combine(pages.map(Page::progressFlow)) { it.average().toInt() }
        }
        .distinctUntilChanged()
        .debounce(50.milliseconds)

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    /** Moves the download to [state]; [statusFlow] observers see it at once. */
    fun transition(state: State) {
        _statusFlow.value = state
    }

    enum class State(val value: Int) {
        NOT_DOWNLOADED(value = 0),
        QUEUE(value = 1),
        DOWNLOADING(value = 2),
        DOWNLOADED(value = 3),
        ERROR(value = 4),
    }

    override fun equals(other: Any?): Boolean = this === other ||
        (other is Download && source == other.source && manga == other.manga && chapter == other.chapter)

    override fun hashCode(): Int = 31 * (31 * source.hashCode() + manga.hashCode()) + chapter.hashCode()

    companion object {
        suspend fun fromChapterId(
            chapterId: Long,
            getChapter: GetChapter = Injekt.get(),
            getManga: GetManga = Injekt.get(),
            sourceManager: SourceManager = Injekt.get(),
        ): Download? {
            val chapter = getChapter.await(chapterId) ?: return null
            val manga = getManga.await(chapter.mangaId) ?: return null
            val source = sourceManager.get(manga.source) as? HttpSource ?: return null

            return Download(source, manga, chapter)
        }
    }
}
