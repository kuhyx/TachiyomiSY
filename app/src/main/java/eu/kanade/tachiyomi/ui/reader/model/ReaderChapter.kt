package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.domain.chapter.model.toDbChapter
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import kotlinx.coroutines.flow.MutableStateFlow
import tachiyomi.core.common.util.system.logcat

/**
 * A chapter as the reader holds it. A mutable state holder (the loader and the viewers advance
 * [state], [pageLoader] and [requestedPage] in place), so not a data class; equality is still by
 * [chapter], which is how the chapter list finds it.
 */
internal class ReaderChapter(val chapter: Chapter) {

    val stateFlow = MutableStateFlow<State>(State.Wait)
    var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    // How many viewers hold the chapter; recycled when the last one lets go.
    private var references = 0

    constructor(chapter: tachiyomi.domain.chapter.model.Chapter) : this(chapter.toDbChapter())

    /** One more viewer holds the chapter. */
    fun ref() {
        references++
    }

    /** One viewer let go; the last one recycles the loader. */
    fun unref() {
        references--
        if (references == 0) {
            if (pageLoader != null) {
                logcat { "Recycling chapter ${chapter.name}" }
            }
            pageLoader?.recycle()
            pageLoader = null
            state = State.Wait
        }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is ReaderChapter && chapter == other.chapter)

    override fun hashCode(): Int = chapter.hashCode()

    sealed interface State {
        data object Wait : State
        data object Loading : State
        data class Error(val error: Throwable) : State
        data class Loaded(val pages: List<ReaderPage>) : State
    }
}
