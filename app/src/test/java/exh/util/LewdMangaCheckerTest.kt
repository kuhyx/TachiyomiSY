package exh.util

import eu.kanade.tachiyomi.source.Source
import exh.source.EH_SOURCE_ID
import exh.source.LEWD_SOURCE_SERIES
import exh.source.nHentaiSourceIds
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

internal class LewdMangaCheckerTest {
    private val sourceManager = mockk<SourceManager>()
    private val savedNHentaiIds = nHentaiSourceIds

    @BeforeEach
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single<SourceManager> { sourceManager } }) }
        every { sourceManager.get(any()) } returns null
        every { sourceManager.get(HENTAI_NAMED) } returns mockk<Source> { every { name } returns "Simply Hentai" }
        every { sourceManager.get(PLAIN_NAMED) } returns mockk<Source> { every { name } returns "MangaDex" }
        nHentaiSourceIds = listOf(NHENTAI)
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        nHentaiSourceIds = savedNHentaiIds
    }

    private fun manga(source: Long, genre: List<String>? = null) = Manga.create().copy(source = source, ogGenre = genre)

    @Test
    fun ehGalleriesAreLewdUnlessNonH() {
        manga(EH_SOURCE_ID).isLewd().shouldBeTrue()
        manga(EH_SOURCE_ID, listOf("female:x", "other:y")).isLewd().shouldBeTrue()
        manga(EH_SOURCE_ID, listOf("reclass:Non-H")).isLewd().shouldBeFalse()
    }

    @Test
    fun nhentaiFollowsTheSameRule() {
        manga(NHENTAI, listOf("tag")).isLewd().shouldBeTrue()
        manga(NHENTAI, listOf("non-h")).isLewd().shouldBeFalse()
    }

    @Test
    fun builtInLewdIdRange() {
        manga(LEWD_SOURCE_SERIES + 5).isLewd().shouldBeTrue()
        manga(LEWD_SOURCE_SERIES + 13).isLewd().shouldBeTrue()
        manga(LEWD_SOURCE_SERIES + 14).isLewd().shouldBeFalse()
    }

    @Test
    fun sourceNameDecides() {
        manga(HENTAI_NAMED).isLewd().shouldBeTrue()
        manga(PLAIN_NAMED).isLewd().shouldBeFalse()
    }

    @Test
    fun genreTagsDecide() {
        manga(PLAIN_NAMED, listOf("Romance", "Smut")).isLewd().shouldBeTrue()
        manga(PLAIN_NAMED, listOf("Romance")).isLewd().shouldBeFalse()
        manga(UNNAMED, listOf("18+")).isLewd().shouldBeTrue()
        manga(UNNAMED).isLewd().shouldBeFalse()
    }

    private companion object {
        const val NHENTAI = 40L
        const val HENTAI_NAMED = 41L
        const val PLAIN_NAMED = 42L
        const val UNNAMED = 43L
    }
}
