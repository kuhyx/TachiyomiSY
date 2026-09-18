package tachiyomi.domain.chapter.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import org.junit.jupiter.api.Test

internal class ChapterTest {

    @Test
    fun createIsBlankRow() {
        val chapter = Chapter.create()
        chapter.id shouldBe -1L
        chapter.mangaId shouldBe -1L
        chapter.read shouldBe false
        chapter.bookmark shouldBe false
        chapter.lastPageRead shouldBe 0L
        chapter.dateFetch shouldBe 0L
        chapter.sourceOrder shouldBe 0L
        chapter.url shouldBe ""
        chapter.name shouldBe ""
        chapter.dateUpload shouldBe -1L
        chapter.chapterNumber shouldBe -1.0
        chapter.scanlator shouldBe null
        chapter.lastModifiedAt shouldBe 0L
        chapter.version shouldBe 1L
        chapter.memo shouldBe JsonObject.EMPTY
    }

    @Test
    fun recognizedNumber() {
        Chapter.create().isRecognizedNumber shouldBe false
        Chapter.create().copy(chapterNumber = 0.0).isRecognizedNumber shouldBe true
        Chapter.create().copy(chapterNumber = 12.5).isRecognizedNumber shouldBe true
    }

    @Test
    fun copyFromTakesSourceFields() {
        val stored = fullChapter()
        val fetched = Chapter.create().copy(
            name = "New name",
            url = "/new",
            dateUpload = 99L,
            chapterNumber = 2.0,
            scanlator = "Other",
        )

        val merged = stored.copyFrom(fetched)

        merged.name shouldBe "New name"
        merged.url shouldBe "/new"
        merged.dateUpload shouldBe 99L
        merged.chapterNumber shouldBe 2.0
        merged.scanlator shouldBe "Other"
        // Identity and reader state are kept.
        merged.id shouldBe 1L
        merged.mangaId shouldBe 2L
        merged.read shouldBe true
        merged.bookmark shouldBe true
        merged.lastPageRead shouldBe 3L
        merged.memo shouldBe sampleMemo
    }

    @Test
    fun copyFromBlankScanlator() {
        fullChapter().copyFrom(Chapter.create().copy(scanlator = "   ")).scanlator shouldBe null
    }

    @Test
    fun copyFromNullScanlator() {
        fullChapter().copyFrom(Chapter.create()).scanlator shouldBe null
    }

    @Test
    fun dataClassContract() {
        val chapter = fullChapter()
        val same = fullChapter()

        chapter shouldBe same
        chapter.hashCode() shouldBe same.hashCode()
        chapter.toString() shouldBe same.toString()
        chapter.toString() shouldContain "Chapter 1"
        chapter.copy() shouldBe chapter
        chapter shouldNotBe chapter.copy(id = 9L)
        chapter shouldNotBe chapter.copy(scanlator = null)
        chapter shouldNotBe "not a chapter"
    }

    @Test
    fun components() {
        val chapter = fullChapter()
        chapter.component1() shouldBe 1L
        chapter.component2() shouldBe 2L
        chapter.component3() shouldBe true
        chapter.component4() shouldBe true
        chapter.component5() shouldBe 3L
        chapter.component6() shouldBe 4L
        chapter.component7() shouldBe 5L
        chapter.component8() shouldBe "/c1"
        chapter.component9() shouldBe "Chapter 1"
        chapter.component10() shouldBe 6L
        chapter.component11() shouldBe 1.0
        chapter.component12() shouldBe "Group"
        chapter.component13() shouldBe 7L
        chapter.component14() shouldBe 8L
        chapter.component15() shouldBe sampleMemo
    }
}
