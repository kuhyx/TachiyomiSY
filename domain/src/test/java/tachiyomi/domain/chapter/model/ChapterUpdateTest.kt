package tachiyomi.domain.chapter.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class ChapterUpdateTest {

    private fun fullUpdate(): ChapterUpdate = ChapterUpdate(
        id = 1L,
        mangaId = 2L,
        read = true,
        bookmark = true,
        lastPageRead = 3L,
        dateFetch = 4L,
        sourceOrder = 5L,
        url = "/c1",
        name = "Chapter 1",
        dateUpload = 6L,
        chapterNumber = 1.0,
        scanlator = "Group",
        version = 8L,
        memo = sampleMemo,
    )

    @Test
    fun defaultsLeaveColumnsAlone() {
        val update = ChapterUpdate(id = 5L)
        update.id shouldBe 5L
        update.mangaId shouldBe null
        update.read shouldBe null
        update.bookmark shouldBe null
        update.lastPageRead shouldBe null
        update.dateFetch shouldBe null
        update.sourceOrder shouldBe null
        update.url shouldBe null
        update.name shouldBe null
        update.dateUpload shouldBe null
        update.chapterNumber shouldBe null
        update.scanlator shouldBe null
        update.version shouldBe null
        update.memo shouldBe null
    }

    @Test
    fun everyColumnIsKept() {
        val update = fullUpdate()
        update.id shouldBe 1L
        update.mangaId shouldBe 2L
        update.read shouldBe true
        update.bookmark shouldBe true
        update.lastPageRead shouldBe 3L
        update.dateFetch shouldBe 4L
        update.sourceOrder shouldBe 5L
        update.url shouldBe "/c1"
        update.name shouldBe "Chapter 1"
        update.dateUpload shouldBe 6L
        update.chapterNumber shouldBe 1.0
        update.scanlator shouldBe "Group"
        update.version shouldBe 8L
        update.memo shouldBe sampleMemo
    }

    @Test
    fun toChapterUpdateRewritesAll() {
        fullChapter().toChapterUpdate() shouldBe fullUpdate()
        Chapter.create().toChapterUpdate() shouldBe ChapterUpdate(
            id = -1L,
            mangaId = -1L,
            read = false,
            bookmark = false,
            lastPageRead = 0L,
            dateFetch = 0L,
            sourceOrder = 0L,
            url = "",
            name = "",
            dateUpload = -1L,
            chapterNumber = -1.0,
            scanlator = null,
            version = 1L,
            memo = Chapter.create().memo,
        )
    }

    @Test
    fun dataClassContract() {
        val update = fullUpdate()
        val same = fullUpdate()

        update shouldBe same
        update.hashCode() shouldBe same.hashCode()
        update.toString() shouldBe same.toString()
        update.toString() shouldContain "Chapter 1"
        update.copy() shouldBe update
        update shouldNotBe update.copy(id = 9L)
        update shouldNotBe ChapterUpdate(id = 1L)
        update shouldNotBe "not an update"
    }

    @Test
    fun components() {
        val update = fullUpdate()
        update.component1() shouldBe 1L
        update.component2() shouldBe 2L
        update.component3() shouldBe true
        update.component4() shouldBe true
        update.component5() shouldBe 3L
        update.component6() shouldBe 4L
        update.component7() shouldBe 5L
        update.component8() shouldBe "/c1"
        update.component9() shouldBe "Chapter 1"
        update.component10() shouldBe 6L
        update.component11() shouldBe 1.0
        update.component12() shouldBe "Group"
        update.component13() shouldBe 8L
        update.component14() shouldBe sampleMemo
    }
}
