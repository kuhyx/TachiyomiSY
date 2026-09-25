package exh.eh

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

internal class ChapterMergeTest {
    private fun chapter(read: Boolean = false, bookmark: Boolean = false, lastPageRead: Long = 0) =
        Chapter.create().copy(id = 3, mangaId = 4, url = "/s/1", name = "v1: x", read = read, bookmark = bookmark)
            .copy(lastPageRead = lastPageRead)

    @Test
    fun mergedWithUnionsFlagsAndPage() {
        val merged = chapter(read = false, bookmark = true, lastPageRead = 2)
            .mergedWith(chapter(read = true, bookmark = false, lastPageRead = 7), newLastPageRead = 9)
        merged.read shouldBe true
        merged.bookmark shouldBe true
        merged.lastPageRead shouldBe 7
        merged.id shouldBe 3
    }

    @Test
    fun mergedWithFallsBackToChainPage() {
        chapter().mergedWith(chapter(), newLastPageRead = 9).lastPageRead shouldBe 9
        chapter().mergedWith(chapter(), newLastPageRead = null).lastPageRead shouldBe 0
        chapter(read = true).mergedWith(chapter(), null).read shouldBe true
        chapter().mergedWith(chapter(bookmark = true), null).bookmark shouldBe true
    }

    @Test
    fun copyIntoResetsIdentity() {
        val copy = chapter(read = true, lastPageRead = 4).copyInto(mangaId = 9, newLastPageRead = 8)
        copy.id shouldBe -1
        copy.mangaId shouldBe 9
        copy.url shouldBe "/s/1"
        copy.read shouldBe true
        copy.lastPageRead shouldBe 4
        copy.chapterNumber shouldBe -1.0
        copy.sourceOrder shouldBe -1
    }

    @Test
    fun copyIntoInheritsChainPage() {
        chapter().copyInto(9, newLastPageRead = 8).lastPageRead shouldBe 8
        chapter().copyInto(9, newLastPageRead = null).lastPageRead shouldBe 0
    }
}
