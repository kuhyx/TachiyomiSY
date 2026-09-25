package eu.kanade.tachiyomi.data.coil

import coil3.Extras
import coil3.decode.DataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class MangaCoverFetcherTest {

    @TempDir
    lateinit var dir: File

    private fun file(name: String, text: String): File = File(dir, name).apply { writeText(text) }

    @Test
    fun customCoverWins() = runTest {
        val custom = file("custom", "mine")
        val result = coverFetcher { customCoverFile = custom }.fetch().sourceResult()
        result.dataSource shouldBe DataSource.DISK
        result.text() shouldBe "mine"
    }

    @Test
    fun customCoverCanBeSkipped() = runTest {
        val custom = file("custom", "mine")
        val noCustom = Extras.Builder().set(MangaCoverFetcher.USE_CUSTOM_COVER_KEY, false).build()
        val result = coverFetcher {
            customCoverFile = custom
            options = coilOptions(extras = noCustom)
        }.fetch().sourceResult()
        result.text() shouldBe "cover"
    }

    @Test
    fun missingUrlFails() = runTest {
        shouldThrow<IllegalStateException> { coverFetcher { url = null }.fetch() }.message shouldBe
            "No cover specified"
    }

    @Test
    fun unknownSchemesAreInvalid() = runTest {
        shouldThrow<IllegalStateException> { coverFetcher { url = "" }.fetch() }.message shouldBe "Invalid image"
        shouldThrow<IllegalStateException> { coverFetcher { url = "ftp://x" }.fetch() }
    }

    @Test
    fun absolutePathIsAFile() = runTest {
        val cover = file("cover", "local")
        coverFetcher { url = cover.absolutePath }.fetch().sourceResult().text() shouldBe "local"
    }

    @Test
    fun fileUrlIsAFile() = runTest {
        val cover = file("cover", "local")
        coverFetcher { url = "file://${cover.absolutePath}" }.fetch().sourceResult().text() shouldBe "local"
    }

    @Test
    fun customPrefixGoesToNetwork() = runTest {
        val cached = file("library", "custom-url")
        val custom = coverFetcher {
            url = "Custom-https://example.org/c.jpg"
            isLibrary = true
            coverFile = cached
        }
        custom.fetch().sourceResult().text() shouldBe "custom-url"
        coverFetcher { url = "HTTPS://example.org/c.jpg" }.fetch().sourceResult().dataSource shouldBe
            DataSource.NETWORK
    }

    @Test
    fun diskCacheKeyIsLazy() {
        coverFetcher { diskCacheKey = lazyOf("k1") }.diskCacheKey shouldBe "k1"
    }
}
