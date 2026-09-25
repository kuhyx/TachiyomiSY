package exh.util

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.BANDWIDTH_HERO
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.WSRV_NL
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.util.DataSaver.Companion.getImage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Response
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

private fun suffixSaver(suffix: String) = object : DataSaver {
    override fun compress(imageUrl: String) = imageUrl + suffix
}

internal class DataSaverTest {
    private val preferences = SourcePreferences(InMemoryPreferenceStore())
    private val source = mockk<Source> { every { id } returns 7L }

    @Test
    fun noneAndExcludedSourcesAreNoOp() {
        DataSaver(source, preferences) shouldBeSameInstanceAs DataSaver.NoOp
        preferences.dataSaverExcludedSources.set(setOf("7"))
        DataSaver(source, preferences) shouldBeSameInstanceAs DataSaver.NoOp
        preferences.dataSaver.set(BANDWIDTH_HERO)
        DataSaver(source, preferences) shouldBeSameInstanceAs DataSaver.NoOp
        preferences.dataSaver.set(WSRV_NL)
        DataSaver(source, preferences) shouldBeSameInstanceAs DataSaver.NoOp
        DataSaver.NoOp.compress("x") shouldBe "x"
    }

    @Test
    fun bandwidthHeroNeedsServer() {
        preferences.dataSaver.set(BANDWIDTH_HERO)
        DataSaver(source, preferences).compress("https://a/b.png") shouldBe "https://a/b.png"
        preferences.dataSaverServer.set("https://proxy/")
        DataSaver(source, preferences).compress("https://proxy/?url=x") shouldBe "https://proxy/?url=x"
    }

    @Test
    fun bandwidthHeroBuildsProxyUrl() {
        preferences.dataSaver.set(BANDWIDTH_HERO)
        preferences.dataSaverServer.set("https://proxy/")
        preferences.dataSaverImageQuality.set(50)
        val saver = DataSaver(source, preferences)
        saver.compress("https://a/b.png") shouldBe "https://proxy/?jpg=0&l=50&bw=0&url=https://a/b.png"
        saver.compress("https://a/b.JPG") shouldBe "https://proxy/?jpg=0&l=50&bw=0&url=https://a/b.JPG"
        saver.compress("https://a/b.jpeg") shouldBe "https://proxy/?jpg=0&l=50&bw=0&url=https://a/b.jpeg"
        // gifs are ignored by default
        saver.compress("https://a/b.gif") shouldBe "https://a/b.gif"
    }

    @Test
    fun bandwidthHeroIgnoreFlags() {
        preferences.dataSaver.set(BANDWIDTH_HERO)
        preferences.dataSaverServer.set("https://proxy")
        preferences.dataSaverIgnoreJpeg.set(true)
        preferences.dataSaverIgnoreGif.set(false)
        preferences.dataSaverImageFormatJpeg.set(true)
        preferences.dataSaverColorBW.set(true)
        preferences.dataSaverImageQuality.set(10)
        val saver = DataSaver(source, preferences)
        saver.compress("https://a/b.jpg") shouldBe "https://a/b.jpg"
        saver.compress("https://a/b.gif") shouldBe "https://proxy/?jpg=1&l=10&bw=1&url=https://a/b.gif"
    }

    @Test
    fun wsrvDefaultsToWebpOutput() {
        preferences.dataSaver.set(WSRV_NL)
        preferences.dataSaverImageQuality.set(60)
        val saver = DataSaver(source, preferences)
        saver.compress("https://a/b.png") shouldBe "https://wsrv.nl/?url=https://a/b.png&output=webp&q=60"
        saver.compress("https://a/b.jpg") shouldBe "https://wsrv.nl/?url=https://a/b.jpg&output=webp&q=60"
        saver.compress("https://a/b.webp") shouldBe "https://wsrv.nl/?url=https://a/b.webp&q=60&n=-1"
        saver.compress("https://a/b.gif") shouldBe "https://a/b.gif"
    }

    @Test
    fun wsrvJpegOutputAndIgnoreFlags() {
        preferences.dataSaver.set(WSRV_NL)
        preferences.dataSaverImageQuality.set(70)
        preferences.dataSaverImageFormatJpeg.set(true)
        preferences.dataSaverIgnoreJpeg.set(true)
        preferences.dataSaverIgnoreGif.set(false)
        val saver = DataSaver(source, preferences)
        saver.compress("https://a/b.png") shouldBe "https://wsrv.nl/?url=https://a/b.png&output=jpg&q=70"
        saver.compress("https://a/b.jpeg") shouldBe "https://a/b.jpeg"
        saver.compress("https://a/b.gif") shouldBe "https://wsrv.nl/?url=https://a/b.gif&output=jpg&q=70&n=-1"
    }

    @Test
    fun getImageCompressesThenRestores() = runTest {
        val response = mockk<Response>()
        val http = mockk<HttpSource>()
        val seen = mutableListOf<String?>()
        coEvery { http.getImage(any(), any()) } answers {
            seen += firstArg<Page>().imageUrl
            response
        }
        val saver = suffixSaver("?compressed")
        val page = Page(0, imageUrl = "https://a/b.png")
        http.getImage(page, dataSaver = saver) shouldBeSameInstanceAs response
        http.getImage(page, existingSize = 5L, dataSaver = saver) shouldBeSameInstanceAs response
        seen shouldBe listOf("https://a/b.png?compressed", "https://a/b.png?compressed")
        page.imageUrl shouldBe "https://a/b.png"
    }

    @Test
    fun getImageWithoutUrlOrOnFailure() = runTest {
        val response = mockk<Response>()
        val http = mockk<HttpSource>()
        coEvery { http.getImage(any(), any()) } returns response
        val noUrl = Page(0)
        http.getImage(noUrl, dataSaver = DataSaver.NoOp) shouldBeSameInstanceAs response
        val failing = mockk<HttpSource>()
        coEvery { failing.getImage(any(), any()) } throws IllegalStateException("down")
        val page = Page(0, imageUrl = "u.png")
        shouldThrow<IllegalStateException> { failing.getImage(page, dataSaver = suffixSaver("c")) }
        page.imageUrl shouldBe "u.png"
    }
}
