package tachiyomi.data.history

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.InjektHarness
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.view.GetLatestHistory
import java.util.Date
import tachiyomi.data.History as HistoryRow
import tachiyomi.view.History as HistoryViewRow

internal class HistoryMapperTest {
    private val harness = InjektHarness()
    private val readAt = Date(1000L)

    // Built per test: the domain model resolves its custom-title lookup through the installed scope.
    private fun expected(): HistoryWithRelations = HistoryWithRelations(
        id = 1L,
        chapterId = 2L,
        mangaId = 3L,
        ogTitle = "Title",
        chapterNumber = 4.5,
        readAt = readAt,
        readDuration = 60L,
        coverData = MangaCover(mangaId = 3L, sourceId = 5L, isMangaFavorite = true, ogUrl = "thumb", lastModified = 6L),
    )

    @BeforeEach
    fun install() = harness.install()

    @AfterEach
    fun uninstall() = harness.uninstall()

    @Test
    fun mapsHistoryRow() {
        HistoryMapper.mapHistory(HistoryRow(_id = 1L, chapter_id = 2L, last_read = readAt, time_read = 60L)) shouldBe
            History(id = 1L, chapterId = 2L, readAt = readAt, readDuration = 60L)
        val reset = HistoryRow(_id = 1L, chapter_id = 2L, last_read = null, time_read = 0L)
        HistoryMapper.mapHistory(reset).readAt shouldBe null
    }

    @Test
    fun mapsHistoryViewRow() {
        val row = HistoryViewRow(
            id = 1L, mangaId = 3L, chapterId = 2L, title = "Title", thumbnailUrl = "thumb", source = 5L,
            favorite = true, cover_last_modified = 6L, chapterNumber = 4.5, readAt = readAt, readDuration = 60L,
        )
        val mapped = HistoryMapper.mapHistoryWithRelations(row)
        mapped shouldBe expected()
        mapped.title shouldBe "Title"
        mapped.coverData.url shouldBe "thumb"
    }

    @Test
    fun mapsLatestHistoryRow() {
        val row = GetLatestHistory(
            id = 1L, mangaId = 3L, chapterId = 2L, title = "Title", thumbnailUrl = null, source = 5L,
            favorite = false, cover_last_modified = 6L, chapterNumber = 4.5, readAt = null, readDuration = 60L,
        )
        val mapped = HistoryMapper.mapLatestHistory(row)
        mapped shouldBe expected().copy(
            readAt = null,
            coverData = MangaCover(
                mangaId = 3L,
                sourceId = 5L,
                isMangaFavorite = false,
                ogUrl = null,
                lastModified = 6L,
            ),
        )
        mapped.coverData.url shouldBe null
    }
}
