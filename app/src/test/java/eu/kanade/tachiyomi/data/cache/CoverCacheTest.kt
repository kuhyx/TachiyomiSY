package eu.kanade.tachiyomi.data.cache

import android.content.Context
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.domain.manga.model.Manga
import java.io.ByteArrayInputStream
import java.io.File

internal class CoverCacheTest {

    @TempDir
    lateinit var root: File

    private fun externalCache(): CoverCache {
        val context = mockk<Context>()
        every { context.getExternalFilesDir(any()) } answers {
            File(root, firstArg<String>()).apply { mkdirs() }
        }
        return CoverCache(context)
    }

    private fun internalCache(): CoverCache {
        val context = mockk<Context>()
        every { context.getExternalFilesDir(any()) } returns null
        every { context.filesDir } returns File(root, "files").apply { mkdirs() }
        return CoverCache(context)
    }

    private fun manga(id: Long, thumbnailUrl: String?): Manga = Manga.create().copy(
        id = id,
        ogThumbnailUrl = thumbnailUrl,
    )

    @Test
    fun coverFileIsHashedUnderCovers() {
        val file = externalCache().getCoverFile("https://example.org/a.jpg")
        file.shouldNotBeNull()
        file.parentFile!!.name shouldBe "covers"
        file.name shouldBe file.name.lowercase()
    }

    @Test
    fun coverFileOfNullUrlIsNull() {
        externalCache().getCoverFile(null).shouldBeNull()
    }

    @Test
    fun customCoverFileUsesCustomDir() {
        val cache = externalCache()
        val file = cache.getCustomCoverFile(7L)
        file.parentFile!!.name shouldBe "custom"
        file.parentFile!!.parentFile!!.name shouldBe "covers"
        cache.getCustomCoverFile(null).name shouldBe cache.getCustomCoverFile(null).name
    }

    @Test
    fun fallsBackToFilesDir() {
        val file = internalCache().getCustomCoverFile(1L)
        file.absolutePath.contains("${File.separator}files${File.separator}") shouldBe true
    }

    @Test
    fun setCustomCoverWritesTheStream() {
        val cache = externalCache()
        cache.setCustomCoverToCache(manga(id = 3L, thumbnailUrl = null), ByteArrayInputStream("png".toByteArray()))
        cache.getCustomCoverFile(3L).readText() shouldBe "png"
    }

    @Test
    fun deleteCustomCoverOutcome() {
        val cache = externalCache()
        cache.deleteCustomCover(4L) shouldBe false
        cache.setCustomCoverToCache(manga(id = 4L, thumbnailUrl = null), ByteArrayInputStream(ByteArray(1)))
        cache.deleteCustomCover(4L) shouldBe true
        cache.getCustomCoverFile(4L).exists() shouldBe false
    }

    @Test
    fun deleteFromCacheCountsCoverOnly() {
        val cache = externalCache()
        val manga = manga(id = 5L, thumbnailUrl = "https://example.org/b.jpg")
        cache.getCoverFile(manga.thumbnailUrl)!!.writeText("x")
        cache.setCustomCoverToCache(manga, ByteArrayInputStream(ByteArray(1)))
        cache.deleteFromCache(manga) shouldBe 1
        cache.getCustomCoverFile(5L).exists() shouldBe true
    }

    @Test
    fun deleteFromCacheCountsBoth() {
        val cache = externalCache()
        val manga = manga(id = 6L, thumbnailUrl = "https://example.org/c.jpg")
        cache.getCoverFile(manga.thumbnailUrl)!!.writeText("x")
        cache.setCustomCoverToCache(manga, ByteArrayInputStream(ByteArray(1)))
        cache.deleteFromCache(manga = manga, deleteCustomCover = true) shouldBe 2
    }

    /** A non-empty directory exists but cannot be deleted, which is the `&&` arm neither other test hits. */
    @Test
    fun undeletableFilesAreNotCounted() {
        val cache = externalCache()
        val manga = manga(id = 7L, thumbnailUrl = "https://example.org/e.jpg")
        cache.getCoverFile(manga.thumbnailUrl)!!.apply { mkdirs() }.resolve("child").writeText("x")
        cache.getCustomCoverFile(7L).apply { mkdirs() }.resolve("child").writeText("x")
        cache.deleteFromCache(manga = manga, deleteCustomCover = true) shouldBe 0
    }

    @Test
    fun deleteFromCacheOfMissingFiles() {
        val cache = externalCache()
        cache.deleteFromCache(manga = manga(id = 8L, thumbnailUrl = null), deleteCustomCover = true) shouldBe 0
        val absent = manga(id = 9L, thumbnailUrl = "https://example.org/d.jpg")
        cache.deleteFromCache(manga = absent, deleteCustomCover = false) shouldBe 0
    }
}
