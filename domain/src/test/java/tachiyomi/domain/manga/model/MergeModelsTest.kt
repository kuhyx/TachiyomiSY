package tachiyomi.domain.manga.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MergeModelsTest {

    private val reference = MergedMangaReference(
        id = 1L,
        isInfoManga = true,
        getChapterUpdates = false,
        chapterSortMode = MergedMangaReference.CHAPTER_SORT_PRIORITY,
        chapterPriority = 2,
        downloadChapters = true,
        mergeId = 3L,
        mergeUrl = "/merge",
        mangaId = 4L,
        mangaUrl = "/manga",
        mangaSourceId = 5L,
    )

    @Test
    fun referenceComponents() {
        reference.component1() shouldBe 1L
        reference.component2() shouldBe true
        reference.component3() shouldBe false
        reference.component4() shouldBe MergedMangaReference.CHAPTER_SORT_PRIORITY
        reference.component5() shouldBe 2
        reference.component6() shouldBe true
        reference.component7() shouldBe 3L
        reference.component8() shouldBe "/merge"
        reference.component9() shouldBe 4L
        reference.component10() shouldBe "/manga"
        reference.component11() shouldBe 5L
    }

    @Test
    fun referenceDataClassMembers() {
        val same = reference.copy()
        val nulls = reference.copy(mergeId = null, mangaId = null)

        same shouldBe reference
        same.hashCode() shouldBe reference.hashCode()
        same.toString() shouldBe reference.toString()
        reference.toString().startsWith("MergedMangaReference(id=1, isInfoManga=true") shouldBe true
        (nulls == reference) shouldBe false
        nulls.mergeId shouldBe null
        nulls.mangaId shouldBe null
    }

    @Test
    fun referenceSortModes() {
        MergedMangaReference.CHAPTER_SORT_NONE shouldBe 0
        MergedMangaReference.CHAPTER_SORT_NO_DEDUPE shouldBe 1
        MergedMangaReference.CHAPTER_SORT_PRIORITY shouldBe 2
        MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS shouldBe 3
        MergedMangaReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER shouldBe 4
    }

    @Test
    fun settingsUpdateMembers() {
        val update = MergeMangaSettingsUpdate(
            id = 1L,
            isInfoManga = true,
            getChapterUpdates = false,
            chapterPriority = 2,
            downloadChapters = true,
            chapterSortMode = 3,
        )
        val (id, isInfoManga, getChapterUpdates) = update

        id shouldBe 1L
        isInfoManga shouldBe true
        getChapterUpdates shouldBe false
        update.component4() shouldBe 2
        update.component5() shouldBe true
        update.component6() shouldBe 3
        update.copy() shouldBe update
        update.copy().hashCode() shouldBe update.hashCode()
        update.toString() shouldBe "MergeMangaSettingsUpdate(id=1, isInfoManga=true, getChapterUpdates=false, " +
            "chapterPriority=2, downloadChapters=true, chapterSortMode=3)"
        (update == update.copy(chapterSortMode = null)) shouldBe false
    }

    @Test
    fun settingsUpdateAllowsNulls() {
        val update = MergeMangaSettingsUpdate(
            id = 1L,
            isInfoManga = null,
            getChapterUpdates = null,
            chapterPriority = null,
            downloadChapters = null,
            chapterSortMode = null,
        )

        update.isInfoManga shouldBe null
        update.getChapterUpdates shouldBe null
        update.chapterPriority shouldBe null
        update.downloadChapters shouldBe null
        update.chapterSortMode shouldBe null
    }

    @Test
    fun mangaWithChapterCountMembers() {
        val manga = MangaFixtures.manga(id = 8L)
        val value = MangaWithChapterCount(manga = manga, chapterCount = 12L)
        val (component1, component2) = value

        component1 shouldBe manga
        component2 shouldBe 12L
        value.copy(chapterCount = 1L).chapterCount shouldBe 1L
        value.copy() shouldBe value
        value.copy().hashCode() shouldBe value.hashCode()
        value.toString().endsWith("chapterCount=12)") shouldBe true
        (value == value.copy(chapterCount = 1L)) shouldBe false
    }

    @Test
    fun customMangaInfoDefaults() {
        val info = CustomMangaInfo(id = 1L, title = "t")

        info.author shouldBe null
        info.artist shouldBe null
        info.thumbnailUrl shouldBe null
        info.description shouldBe null
        info.genre shouldBe null
        info.status shouldBe null
    }

    @Test
    fun customMangaInfoMembers() {
        val info = MangaFixtures.fullCustomInfo
        val (id, title, author) = info

        id shouldBe MangaFixtures.CUSTOM_ID
        title shouldBe "Custom title"
        author shouldBe "Custom author"
        info.component4() shouldBe "Custom artist"
        info.component5() shouldBe "https://example.com/custom.png"
        info.component6() shouldBe "Custom description"
        info.component7() shouldBe listOf("Custom genre")
        info.component8() shouldBe 5L
        info.copy() shouldBe info
        info.copy().hashCode() shouldBe info.hashCode()
        info.toString().startsWith("CustomMangaInfo(id=${MangaFixtures.CUSTOM_ID}, title=Custom title") shouldBe true
        (info == info.copy(status = null)) shouldBe false
    }
}
