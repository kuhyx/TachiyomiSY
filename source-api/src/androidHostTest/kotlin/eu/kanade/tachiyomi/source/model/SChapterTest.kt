package eu.kanade.tachiyomi.source.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

internal class SChapterTest {
    @Test
    fun urlAndNameAreLateinit() {
        val chapter = SChapterImpl()
        shouldThrow<UninitializedPropertyAccessException> { chapter.url }
        shouldThrow<UninitializedPropertyAccessException> { chapter.name }
        chapter.url = "/c"
        chapter.name = "Chapter"
        chapter.url shouldBe "/c"
        chapter.name shouldBe "Chapter"
    }

    @Test
    fun implDefaults() {
        val chapter = SChapterImpl()
        chapter.chapter_number shouldBe -1f
        chapter.scanlator shouldBe null
        chapter.date_upload shouldBe 0L
        chapter.memo shouldBe JsonObject(emptyMap())
    }

    @Test
    fun implSetters() {
        val chapter = SChapterImpl()
        chapter.chapter_number = 2.5f
        chapter.scanlator = "Group"
        chapter.date_upload = 123L
        chapter.memo = buildJsonObject { put("k", 1) }
        chapter.chapter_number shouldBe 2.5f
        chapter.scanlator shouldBe "Group"
        chapter.date_upload shouldBe 123L
        chapter.memo shouldBe buildJsonObject { put("k", 1) }
    }

    @Test
    fun createReturnsImpl() {
        SChapter.create().shouldBeInstanceOf<SChapterImpl>()
    }

    @Test
    fun invokeDefaults() {
        val chapter = SChapter(name = "Chapter", url = "/c")
        chapter.name shouldBe "Chapter"
        chapter.url shouldBe "/c"
        chapter.date_upload shouldBe 0L
        chapter.chapter_number shouldBe -1f
        chapter.scanlator shouldBe null
    }

    @Test
    fun invokeAllArguments() {
        val chapter = SChapter(
            name = "Chapter",
            url = "/c",
            dateUpload = 42L,
            chapterNumber = 3f,
            scanlator = "Group",
        )
        chapter.date_upload shouldBe 42L
        chapter.chapter_number shouldBe 3f
        chapter.scanlator shouldBe "Group"
    }

    @Test
    fun invokeEachOptional() {
        SChapter(name = "n", url = "/u", dateUpload = 7L).date_upload shouldBe 7L
        SChapter(name = "n", url = "/u", chapterNumber = 9f).chapter_number shouldBe 9f
        SChapter(name = "n", url = "/u", scanlator = "s").scanlator shouldBe "s"
    }

    @Test
    fun copyFromCopiesEveryField() {
        val source = SChapter(
            name = "Chapter",
            url = "/c",
            dateUpload = 42L,
            chapterNumber = 3f,
            scanlator = "Group",
        )
        source.memo = buildJsonObject { put("k", 1) }
        val target = SChapter.create()
        target.copyFrom(source)
        target.name shouldBe "Chapter"
        target.url shouldBe "/c"
        target.date_upload shouldBe 42L
        target.chapter_number shouldBe 3f
        target.scanlator shouldBe "Group"
        target.memo shouldBe source.memo
    }

    @Test
    fun copyFromOverwritesNullable() {
        val target = SChapter(name = "old", url = "/old", scanlator = "keep?")
        target.copyFrom(SChapter(name = "new", url = "/new"))
        target.scanlator shouldBe null
        target.name shouldBe "new"
    }
}
