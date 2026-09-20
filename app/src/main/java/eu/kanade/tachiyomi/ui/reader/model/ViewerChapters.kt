package eu.kanade.tachiyomi.ui.reader.model

internal data class ViewerChapters(
    val currChapter: ReaderChapter,
    val prevChapter: ReaderChapter?,
    val nextChapter: ReaderChapter?,
)

internal fun ViewerChapters.ref() {
    currChapter.ref()
    prevChapter?.ref()
    nextChapter?.ref()
}

internal fun ViewerChapters.unref() {
    currChapter.unref()
    prevChapter?.unref()
    nextChapter?.unref()
}
