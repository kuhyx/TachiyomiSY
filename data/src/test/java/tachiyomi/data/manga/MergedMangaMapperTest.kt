package tachiyomi.data.manga

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.Merged

internal class MergedMangaMapperTest {
    @Test
    fun mapCopiesEveryColumn() {
        val row = Merged(
            _id = 1L, info_manga = true, get_chapter_updates = false, chapter_sort_mode = 2L, chapter_priority = 3L,
            download_chapters = true, merge_id = 4L, merge_url = "/merge", manga_id = 5L, manga_url = "/manga",
            manga_source = 6L,
        )

        val reference = MergedMangaMapper.map(row)

        reference.id shouldBe 1L
        reference.isInfoManga shouldBe true
        reference.getChapterUpdates shouldBe false
        reference.chapterSortMode shouldBe 2
        reference.chapterPriority shouldBe 3
        reference.downloadChapters shouldBe true
        reference.mergeId shouldBe 4L
        reference.mergeUrl shouldBe "/merge"
        reference.mangaId shouldBe 5L
        reference.mangaUrl shouldBe "/manga"
        reference.mangaSourceId shouldBe 6L
    }

    @Test
    fun mapKeepsNullMangaId() {
        val row = Merged(
            _id = 1L, info_manga = false, get_chapter_updates = true, chapter_sort_mode = 0L, chapter_priority = 0L,
            download_chapters = false, merge_id = 4L, merge_url = "/merge", manga_id = null, manga_url = "/manga",
            manga_source = 6L,
        )

        MergedMangaMapper.map(row).mangaId shouldBe null
    }
}
