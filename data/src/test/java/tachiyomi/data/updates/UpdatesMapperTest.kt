package tachiyomi.data.updates

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.InjektHarness
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.view.UpdatesView

internal class UpdatesMapperTest {
    private val harness = InjektHarness()

    @BeforeEach
    fun install() = harness.install()

    @AfterEach
    fun uninstall() = harness.uninstall()

    @Test
    fun mapsEveryColumn() {
        val row = UpdatesView(
            mangaId = 1L, mangaTitle = "Title", chapterId = 2L, chapterName = "Chapter", scanlator = "Group",
            chapterUrl = "/c/2", read = true, bookmark = false, last_page_read = 3L, source = 4L, favorite = true,
            thumbnailUrl = "thumb", coverLastModified = 5L, dateUpload = 6L, datefetch = 7L, excludedScanlator = null,
        )
        val mapped = UpdatesMapper.mapUpdates(row)
        mapped shouldBe UpdatesWithRelations(
            mangaId = 1L, ogMangaTitle = "Title", chapterId = 2L, chapterName = "Chapter", scanlator = "Group",
            chapterUrl = "/c/2", read = true, bookmark = false, lastPageRead = 3L, sourceId = 4L, dateFetch = 7L,
            coverData = MangaCover(
                mangaId = 1L,
                sourceId = 4L,
                isMangaFavorite = true,
                ogUrl = "thumb",
                lastModified = 5L,
            ),
        )
        mapped.mangaTitle shouldBe "Title"
        mapped.coverData.url shouldBe "thumb"
    }

    @Test
    fun keepsNullScanlatorAndCover() {
        val row = UpdatesView(
            mangaId = 1L, mangaTitle = "Title", chapterId = 2L, chapterName = "Chapter", scanlator = null,
            chapterUrl = "/c/2", read = false, bookmark = true, last_page_read = 0L, source = 4L, favorite = false,
            thumbnailUrl = null, coverLastModified = 0L, dateUpload = 0L, datefetch = 0L, excludedScanlator = "Bad",
        )
        val mapped = UpdatesMapper.mapUpdates(row)
        mapped.scanlator shouldBe null
        mapped.bookmark shouldBe true
        mapped.coverData.url shouldBe null
    }
}
