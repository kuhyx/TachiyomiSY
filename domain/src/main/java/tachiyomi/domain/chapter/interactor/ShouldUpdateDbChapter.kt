package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.model.Chapter

/** Decides whether a chapter fetched from the source differs from its stored row. */
public class ShouldUpdateDbChapter {

    /**
     * Whether any source-reported field of [sourceChapter] differs from [dbChapter]; read state and
     * bookmarks are ignored.
     */
    public fun await(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
        return dbChapter.scanlator != sourceChapter.scanlator ||
            dbChapter.name != sourceChapter.name ||
            dbChapter.dateUpload != sourceChapter.dateUpload ||
            dbChapter.chapterNumber != sourceChapter.chapterNumber ||
            dbChapter.sourceOrder != sourceChapter.sourceOrder ||
            dbChapter.memo != sourceChapter.memo
    }
}
