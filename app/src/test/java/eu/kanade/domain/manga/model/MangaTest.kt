package eu.kanade.domain.manga.model

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga
import java.io.File
import kotlin.io.path.createTempFile

internal class MangaTest {

    private val basePreferences = BasePreferences(mockk(relaxed = true), InMemoryPreferenceStore())
    private val coverCache = mockk<CoverCache>()

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { basePreferences }
                    single { coverCache }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun viewerFlagsSplitInTwo() {
        val manga = Manga.create().copy(viewerFlags = ReadingMode.WEBTOON.flagValue.toLong() or 0x00000010L)
        manga.readingMode shouldBe ReadingMode.WEBTOON.flagValue.toLong()
        manga.readerOrientation shouldBe (0x00000010L and ReaderOrientation.MASK.toLong())
    }

    @Test
    fun downloadedFilterFollowsFlags() {
        Manga.create().downloadedFilter shouldBe TriState.DISABLED
        Manga.create().copy(chapterFlags = Manga.CHAPTER_SHOW_DOWNLOADED).downloadedFilter shouldBe TriState.ENABLED_IS
        Manga.create().copy(chapterFlags = Manga.CHAPTER_SHOW_NOT_DOWNLOADED).downloadedFilter shouldBe
            TriState.ENABLED_NOT
    }

    @Test
    fun downloadedOnlyForcesTheFilter() {
        basePreferences.downloadedOnly.set(true)
        Manga.create().downloadedFilter shouldBe TriState.ENABLED_IS
        Manga.create().chaptersFiltered() shouldBe true
    }

    @Test
    fun chaptersFilteredByAnyFilter() {
        Manga.create().chaptersFiltered() shouldBe false
        Manga.create().copy(chapterFlags = Manga.CHAPTER_SHOW_UNREAD).chaptersFiltered() shouldBe true
        Manga.create().copy(chapterFlags = Manga.CHAPTER_SHOW_DOWNLOADED).chaptersFiltered() shouldBe true
        Manga.create().copy(chapterFlags = Manga.CHAPTER_SHOW_BOOKMARKED).chaptersFiltered() shouldBe true
    }

    @Test
    fun convertsToSManga() {
        val manga = Manga.create().copy(
            url = "/m",
            ogTitle = "T",
            ogArtist = "A",
            ogAuthor = "W",
            ogDescription = "D",
            ogGenre = listOf("x", "y"),
            ogStatus = 2,
            ogThumbnailUrl = "http://c",
            initialized = true,
        )
        val sManga = manga.toSManga()
        sManga.url shouldBe "/m"
        sManga.title shouldBe "T"
        sManga.artist shouldBe "A"
        sManga.author shouldBe "W"
        sManga.description shouldBe "D"
        sManga.genre shouldBe "x, y"
        sManga.status shouldBe 2
        sManga.thumbnail_url shouldBe "http://c"
        sManga.initialized shouldBe true
        Manga.create().toSManga().genre shouldBe ""
    }

    @Test
    fun copyFromKeepsLocalOnNulls() {
        val manga = Manga.create().copy(
            ogAuthor = "W",
            ogArtist = "A",
            ogThumbnailUrl = "t",
            ogDescription = "D",
            ogGenre = listOf("g"),
            initialized = true,
        )
        val empty = SManga.create().apply { status = 3 }
        val copied = manga.copyFrom(empty)
        copied.ogAuthor shouldBe "W"
        copied.ogArtist shouldBe "A"
        copied.ogThumbnailUrl shouldBe "t"
        copied.ogDescription shouldBe "D"
        copied.ogGenre shouldBe listOf("g")
        copied.ogStatus shouldBe 3L
        copied.initialized shouldBe false
    }

    @Test
    fun copyFromTakesRemoteValues() {
        val remote = SManga.create().apply {
            author = "W2"
            artist = "A2"
            thumbnail_url = "t2"
            description = "D2"
            genre = "a, b"
            status = 1
            update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
            initialized = true
        }
        val copied = Manga.create().copy(initialized = true).copyFrom(remote)
        copied.ogAuthor shouldBe "W2"
        copied.ogArtist shouldBe "A2"
        copied.ogThumbnailUrl shouldBe "t2"
        copied.ogDescription shouldBe "D2"
        copied.ogGenre shouldBe listOf("a", "b")
        copied.updateStrategy shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        copied.initialized shouldBe true
        Manga.create().copyFrom(remote).initialized shouldBe false
    }

    @Test
    fun customCoverIsAFileOnDisk() {
        val existing = createTempFile("cover").toFile()
        val missing = File("missing-custom-cover")
        every { coverCache.getCustomCoverFile(1) } returns existing
        every { coverCache.getCustomCoverFile(2) } returns missing
        Manga.create().copy(id = 1).hasCustomCover() shouldBe true
        Manga.create().copy(id = 2).hasCustomCover(coverCache) shouldBe false
    }
}
