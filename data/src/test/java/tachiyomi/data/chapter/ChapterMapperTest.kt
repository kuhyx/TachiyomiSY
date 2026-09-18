package tachiyomi.data.chapter

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import tachiyomi.data.Chapters
import tachiyomi.domain.chapter.model.Chapter

internal class ChapterMapperTest {
    @Test
    fun mapsEveryColumn() {
        val memo = JsonObject(mapOf("k" to JsonPrimitive("v")))
        val row = Chapters(
            _id = 1L, manga_id = 2L, url = "/c/1", name = "Chapter 1", scanlator = "Group", read = true,
            bookmark = false, last_page_read = 3L, chapter_number = 1.5, source_order = 4L, date_fetch = 5L,
            date_upload = 6L, last_modified_at = 7L, version = 8L, is_syncing = 1L, memo = memo,
        )
        ChapterMapper.mapChapter(row) shouldBe Chapter(
            id = 1L, mangaId = 2L, read = true, bookmark = false, lastPageRead = 3L, dateFetch = 5L,
            sourceOrder = 4L, url = "/c/1", name = "Chapter 1", dateUpload = 6L, chapterNumber = 1.5,
            scanlator = "Group", lastModifiedAt = 7L, version = 8L, memo = memo,
        )
    }

    @Test
    fun keepsNullScanlator() {
        val row = Chapters(
            _id = 1L, manga_id = 2L, url = "/c/1", name = "Chapter 1", scanlator = null, read = false,
            bookmark = true, last_page_read = 0L, chapter_number = -1.0, source_order = 0L, date_fetch = 0L,
            date_upload = -1L, last_modified_at = 0L, version = 0L, is_syncing = 0L, memo = JsonObject(emptyMap()),
        )
        val chapter = ChapterMapper.mapChapter(row)
        chapter.scanlator shouldBe null
        chapter.bookmark shouldBe true
        chapter.isRecognizedNumber shouldBe false
    }
}
