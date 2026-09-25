package eu.kanade.domain.chapter.model

import eu.kanade.tachiyomi.source.model.SChapter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

internal class ChapterTest {

    private val memo = JsonObject(mapOf("k" to JsonPrimitive("v")))
    private val chapter = Chapter.create().copy(
        id = 3,
        mangaId = 4,
        url = "/c",
        name = "Name",
        scanlator = "Group",
        read = true,
        bookmark = true,
        lastPageRead = 5,
        dateFetch = 6,
        dateUpload = 7,
        chapterNumber = 8.5,
        sourceOrder = 9,
        lastModifiedAt = 10,
        memo = memo,
    )

    @Test
    fun convertsToSChapter() {
        val sChapter = chapter.toSChapter()
        sChapter.url shouldBe "/c"
        sChapter.name shouldBe "Name"
        sChapter.date_upload shouldBe 7L
        sChapter.chapter_number shouldBe 8.5f
        sChapter.scanlator shouldBe "Group"
        sChapter.memo shouldBe memo
    }

    @Test
    fun copiesFromSChapter() {
        val sChapter = SChapter.create().apply {
            url = "/n"
            name = "New"
            date_upload = 70
            chapter_number = 1.5f
            scanlator = " Team "
        }
        val copied = chapter.copyFromSChapter(sChapter)
        copied.url shouldBe "/n"
        copied.name shouldBe "New"
        copied.dateUpload shouldBe 70L
        copied.chapterNumber shouldBe 1.5
        copied.scanlator shouldBe "Team"
        copied.id shouldBe 3L
        chapter.copyFromSChapter(sChapter.apply { scanlator = "  " }).scanlator.shouldBeNull()
        chapter.copyFromSChapter(sChapter.apply { scanlator = null }).scanlator.shouldBeNull()
    }

    @Test
    fun convertsToDbChapter() {
        val db = chapter.toDbChapter()
        db.id shouldBe 3L
        db.mangaId shouldBe 4L
        db.url shouldBe "/c"
        db.name shouldBe "Name"
        db.scanlator shouldBe "Group"
        db.read shouldBe true
        db.bookmark shouldBe true
        db.lastPageRead shouldBe 5
        db.dateFetch shouldBe 6L
        db.date_upload shouldBe 7L
        db.chapter_number shouldBe 8.5f
        db.sourceOrder shouldBe 9
        db.lastModified shouldBe 10L
        db.memo shouldBe memo
    }
}
