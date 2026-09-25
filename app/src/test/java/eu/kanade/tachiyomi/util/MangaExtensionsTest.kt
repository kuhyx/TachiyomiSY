package eu.kanade.tachiyomi.util

import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import java.io.ByteArrayInputStream

internal class MangaExtensionsTest {

    private val coverCache = mockk<CoverCache>()
    private val updateManga = mockk<UpdateManga>()
    private val coverManager = mockk<LocalCoverManager>()
    private val manga = Manga.create().copy(id = 4, source = 7, ogTitle = "T")

    @BeforeEach
    fun setUp() {
        val customMangaRepository = mockk<CustomMangaRepository>()
        every { customMangaRepository.get(any()) } returns null
        every { customMangaRepository.set(any<CustomMangaInfo>()) } returns Unit
        startKoin {
            modules(
                module {
                    single { coverCache }
                    single { updateManga }
                    single { GetCustomMangaInfo(customMangaRepository) }
                },
            )
        }
        coEvery { updateManga.awaitUpdateCoverLastModified(4) } returns true
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun removeCoversStampsWhen() {
        manga.copy(source = LocalSource.ID).removeCovers() shouldBe manga.copy(source = LocalSource.ID)
        every { coverCache.deleteFromCache(manga, true) } returns 0
        manga.removeCovers() shouldBe manga
        every { coverCache.deleteFromCache(manga, true) } returns 2
        (manga.removeCovers(coverCache).coverLastModified > 0L) shouldBe true
    }

    @Test
    fun editCoverGoesToTheLocalFolder() = runTest {
        val local = manga.copy(source = LocalSource.ID)
        every { coverManager.update(any(), any(), any()) } returns null
        local.editCover(coverManager, ByteArrayInputStream(ByteArray(0)))
        verify(exactly = 1) { coverManager.update(any(), any(), false) }
        coVerify(exactly = 1) { updateManga.awaitUpdateCoverLastModified(4) }
    }

    @Test
    fun editCoverGoesToTheCacheFor() = runTest {
        every { coverCache.setCustomCoverToCache(any(), any()) } returns Unit
        manga.copy(favorite = true).editCover(coverManager, ByteArrayInputStream(ByteArray(0)), updateManga, coverCache)
        verify(exactly = 1) { coverCache.setCustomCoverToCache(any(), any()) }
        coVerify(exactly = 1) { updateManga.awaitUpdateCoverLastModified(4) }
        manga.editCover(coverManager, ByteArrayInputStream(ByteArray(0)))
        coVerify(exactly = 1) { updateManga.awaitUpdateCoverLastModified(4) }
    }
}
