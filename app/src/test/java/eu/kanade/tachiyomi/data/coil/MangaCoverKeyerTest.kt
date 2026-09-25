package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import java.io.File

internal class MangaCoverKeyerTest {

    private val existing: File = mockk<File>().also { every { it.exists() } returns true }
    private val missing: File = mockk<File>().also { every { it.exists() } returns false }
    private val coverCache: CoverCache = mockk()
    private val options: Options = Options(context = mockk<Context>(relaxed = true))

    private fun manga(id: Long, thumbnailUrl: String?, lastModified: Long): Manga = Manga.create().copy(
        id = id,
        ogThumbnailUrl = thumbnailUrl,
        coverLastModified = lastModified,
    )

    private fun cover(mangaId: Long, url: String?, lastModified: Long): MangaCover = MangaCover(
        mangaId = mangaId,
        sourceId = 1L,
        isMangaFavorite = false,
        ogUrl = url,
        lastModified = lastModified,
    )

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { coverCache } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun mangaKeyerIdWithCustomCover() {
        every { coverCache.getCustomCoverFile(1L) } returns existing
        MangaKeyer().key(data = manga(id = 1L, thumbnailUrl = "u", lastModified = 9L), options = options) shouldBe "1;9"
    }

    @Test
    fun mangaKeyerUsesUrlWithoutCustom() {
        every { coverCache.getCustomCoverFile(2L) } returns missing
        val manga = manga(id = 2L, thumbnailUrl = "https://a/b.jpg", lastModified = 3L)
        MangaKeyer().key(data = manga, options = options) shouldBe "https://a/b.jpg;3"
    }

    @Test
    fun mangaKeyerKeepsNullUrl() {
        every { coverCache.getCustomCoverFile(4L) } returns missing
        MangaKeyer().key(data = manga(id = 4L, thumbnailUrl = null, lastModified = 0L), options = options) shouldBe
            "null;0"
    }

    @Test
    fun coverKeyerIdWithCustomCover() {
        every { coverCache.getCustomCoverFile(5L) } returns existing
        val keyer = MangaCoverKeyer(coverCache)
        keyer.key(data = cover(mangaId = 5L, url = "u", lastModified = 7L), options = options) shouldBe "5;7"
    }

    @Test
    fun coverKeyerUsesUrlWithoutCustom() {
        every { coverCache.getCustomCoverFile(6L) } returns missing
        val keyer = MangaCoverKeyer(coverCache)
        keyer.key(data = cover(mangaId = 6L, url = "u", lastModified = 8L), options = options) shouldBe "u;8"
    }

    @Test
    fun coverKeyerTakesCacheFromInjekt() {
        every { coverCache.getCustomCoverFile(10L) } returns missing
        MangaCoverKeyer().key(data = cover(mangaId = 10L, url = null, lastModified = 1L), options = options) shouldBe
            "null;1"
    }
}
