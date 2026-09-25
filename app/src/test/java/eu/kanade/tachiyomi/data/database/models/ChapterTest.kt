package eu.kanade.tachiyomi.data.database.models

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

private fun chapter(
    id: Long? = 1L,
    mangaId: Long? = 2L,
    chapterNumber: Float = 1f,
): ChapterImpl = ChapterImpl().also {
    it.id = id
    it.mangaId = mangaId
    it.url = "/c/1"
    it.name = "Chapter 1"
    it.scanlator = "scans"
    it.read = true
    it.bookmark = true
    it.lastPageRead = 4
    it.dateFetch = 100L
    it.date_upload = 200L
    it.chapter_number = chapterNumber
    it.sourceOrder = 3
    it.lastModified = 300L
    it.version = 5L
    it.memo = JsonObject(mapOf("k" to JsonPrimitive("v")))
}

internal class ChapterTest {

    @Test
    fun negativeNumberIsNotRecognized() {
        chapter(chapterNumber = -1f).isRecognizedNumber shouldBe false
    }

    @Test
    fun zeroAndAboveAreRecognized() {
        chapter(chapterNumber = 0f).isRecognizedNumber shouldBe true
        chapter(chapterNumber = 2.5f).isRecognizedNumber shouldBe true
    }

    @Test
    fun toDomainCopiesEveryField() {
        val domain = chapter().toDomainChapter()!!
        domain.id shouldBe 1L
        domain.mangaId shouldBe 2L
        domain.read shouldBe true
        domain.bookmark shouldBe true
        domain.lastPageRead shouldBe 4L
        domain.dateFetch shouldBe 100L
        domain.sourceOrder shouldBe 3L
    }

    @Test
    fun toDomainCopiesSourceFields() {
        val domain = chapter(chapterNumber = 7.5f).toDomainChapter()!!
        domain.url shouldBe "/c/1"
        domain.name shouldBe "Chapter 1"
        domain.dateUpload shouldBe 200L
        domain.chapterNumber shouldBe 7.5
        domain.scanlator shouldBe "scans"
        domain.lastModifiedAt shouldBe 300L
        domain.version shouldBe 5L
        domain.memo shouldBe JsonObject(mapOf("k" to JsonPrimitive("v")))
    }

    @Test
    fun toDomainNeedsBothIds() {
        chapter(id = null).toDomainChapter().shouldBeNull()
        chapter(mangaId = null).toDomainChapter().shouldBeNull()
        chapter(id = null, mangaId = null).toDomainChapter().shouldBeNull()
    }

    @Test
    fun defaultsAreEmpty() {
        val fresh = ChapterImpl()
        fresh.id.shouldBeNull()
        fresh.mangaId.shouldBeNull()
        fresh.url shouldBe ""
        fresh.name shouldBe ""
        fresh.scanlator.shouldBeNull()
        fresh.read shouldBe false
        fresh.bookmark shouldBe false
        fresh.lastPageRead shouldBe 0
    }

    @Test
    fun remainingDefaultsAreZero() {
        val fresh = ChapterImpl()
        fresh.dateFetch shouldBe 0L
        fresh.date_upload shouldBe 0L
        fresh.chapter_number shouldBe 0f
        fresh.sourceOrder shouldBe 0
        fresh.lastModified shouldBe 0L
        fresh.version shouldBe 0L
        fresh.memo shouldBe JsonObject(emptyMap())
    }

    @Test
    fun equalsIsUrlAndId() {
        val one = chapter()
        one.equals(one) shouldBe true
        (one == chapter()) shouldBe true
        (one == chapter(id = 9L)) shouldBe false
        (one == chapter().also { it.url = "/other" }) shouldBe false
    }

    @Test
    fun equalsRejectsOtherTypes() {
        val one = chapter()
        val nothing: Any? = null
        one.equals(nothing) shouldBe false
        one.equals("not a chapter") shouldBe false
    }

    @Test
    fun hashCodeCombinesUrlAndId() {
        chapter().hashCode() shouldBe chapter().hashCode()
        chapter().hashCode() shouldBe "/c/1".hashCode() + 1L.hashCode()
        ChapterImpl().hashCode() shouldBe "".hashCode() + null.hashCode()
    }
}
