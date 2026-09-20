package eu.kanade.presentation.manga

/** @property nextChapters how many unread chapters to queue, or null when the action is not a count. */
internal enum class DownloadAction(val nextChapters: Int? = null) {
    NEXT_1_CHAPTER(nextChapters = 1),
    NEXT_5_CHAPTERS(nextChapters = 5),
    NEXT_10_CHAPTERS(nextChapters = 10),
    NEXT_25_CHAPTERS(nextChapters = 25),
    UNREAD_CHAPTERS,
    BOOKMARKED_CHAPTERS,
}

internal enum class EditCoverAction {
    EDIT,
    DELETE,
}

internal enum class MangaScreenItem {
    INFO_BOX,
    ACTION_ROW,

    // SY -->
    METADATA_INFO,

    // SY <--
    DESCRIPTION_WITH_TAG,

    // SY -->
    INFO_BUTTONS,
    CHAPTER_PREVIEW_LOADING,
    CHAPTER_PREVIEW_ROW,
    CHAPTER_PREVIEW_MORE,

    // SY <--
    CHAPTER_HEADER,
    CHAPTER,
}
