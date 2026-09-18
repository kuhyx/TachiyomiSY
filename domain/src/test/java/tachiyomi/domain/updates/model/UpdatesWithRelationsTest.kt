package tachiyomi.domain.updates.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.CustomMangaInfoScope
import tachiyomi.domain.manga.model.MangaCover

internal class UpdatesWithRelationsTest {

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
        updatesRow(mangaId = CustomMangaInfoScope.PLAIN_MANGA_ID).mangaTitle shouldBe "original"
    }

    @Test
    fun titleIsOgTitleWhenEditNull() {
        updatesRow(mangaId = CustomMangaInfoScope.NULL_TITLE_MANGA_ID).mangaTitle shouldBe "original"
    }

    @Test
    fun titleIsCustomWhenEdited() {
        val row = updatesRow(mangaId = CustomMangaInfoScope.CUSTOM_TITLE_MANGA_ID)

        row.mangaTitle shouldBe CustomMangaInfoScope.CUSTOM_TITLE
    }

    @Test
    fun dataClassSurface() {
        val row = updatesRow(mangaId = CustomMangaInfoScope.PLAIN_MANGA_ID)

        row.component1() shouldBe CustomMangaInfoScope.PLAIN_MANGA_ID
        row.component2() shouldBe "original"
        row.component3() shouldBe 2L
        row.component4() shouldBe "Chapter 1"
        row.component5() shouldBe "group"
        row.component6() shouldBe "/chapter/1"
        row.component7() shouldBe false
        row.component8() shouldBe true
        row.component9() shouldBe 3L
        row.component10() shouldBe 4L
        row.component11() shouldBe 5L
        row.component12() shouldBe row.coverData
        row shouldBe row.copy()
        row.copy(scanlator = null) shouldNotBe row
        row.hashCode() shouldBe row.copy().hashCode()
        row.toString() shouldBe "UpdatesWithRelations(mangaId=13, ogMangaTitle=original, chapterId=2, " +
            "chapterName=Chapter 1, scanlator=group, chapterUrl=/chapter/1, read=false, bookmark=true, " +
            "lastPageRead=3, sourceId=4, dateFetch=5, coverData=${row.coverData})"
    }
}

/** A feed row of manga [mangaId]. */
internal fun updatesRow(mangaId: Long): UpdatesWithRelations = UpdatesWithRelations(
    mangaId = mangaId,
    ogMangaTitle = "original",
    chapterId = 2L,
    chapterName = "Chapter 1",
    scanlator = "group",
    chapterUrl = "/chapter/1",
    read = false,
    bookmark = true,
    lastPageRead = 3L,
    sourceId = 4L,
    dateFetch = 5L,
    coverData = MangaCover(
        mangaId = mangaId,
        sourceId = 4L,
        isMangaFavorite = false,
        ogUrl = null,
        lastModified = 0L,
    ),
)
