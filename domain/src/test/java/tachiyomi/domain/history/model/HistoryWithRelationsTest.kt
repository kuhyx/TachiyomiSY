package tachiyomi.domain.history.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaCover
import java.util.Date

internal class HistoryWithRelationsTest {

    @BeforeEach
    fun beforeEach() {
        CustomMangaInfoScope.install()
    }

    @AfterEach
    fun afterEach() {
        CustomMangaInfoScope.restore()
    }

    @Test
    fun titleIsOgTitleWithoutEdits() {
        val row = historyRow(mangaId = CustomMangaInfoScope.PLAIN_MANGA_ID)

        row.title shouldBe "original"
    }

    @Test
    fun titleIsOgTitleWhenEditNull() {
        val row = historyRow(mangaId = CustomMangaInfoScope.NULL_TITLE_MANGA_ID)

        row.title shouldBe "original"
    }

    @Test
    fun titleIsCustomWhenEdited() {
        val row = historyRow(mangaId = CustomMangaInfoScope.CUSTOM_TITLE_MANGA_ID)

        row.title shouldBe CustomMangaInfoScope.CUSTOM_TITLE
    }

    @Test
    fun dataClassSurface() {
        val row = historyRow(mangaId = CustomMangaInfoScope.PLAIN_MANGA_ID)

        row.component1() shouldBe 1L
        row.component2() shouldBe 2L
        row.component3() shouldBe CustomMangaInfoScope.PLAIN_MANGA_ID
        row.component4() shouldBe "original"
        row.component5() shouldBe 3.5
        row.component6() shouldBe Date(4L)
        row.component7() shouldBe 5L
        row.component8() shouldBe row.coverData
        row shouldBe row.copy()
        row.copy(id = 9L) shouldNotBe row
        row.hashCode() shouldBe row.copy().hashCode()
        row.toString() shouldBe "HistoryWithRelations(id=1, chapterId=2, mangaId=13, ogTitle=original, " +
            "chapterNumber=3.5, readAt=${Date(4L)}, readDuration=5, coverData=${row.coverData})"
    }
}

private fun historyRow(mangaId: Long): HistoryWithRelations = HistoryWithRelations(
    id = 1L,
    chapterId = 2L,
    mangaId = mangaId,
    ogTitle = "original",
    chapterNumber = 3.5,
    readAt = Date(4L),
    readDuration = 5L,
    coverData = MangaCover(
        mangaId = mangaId,
        sourceId = 6L,
        isMangaFavorite = false,
        ogUrl = null,
        lastModified = 0L,
    ),
)
