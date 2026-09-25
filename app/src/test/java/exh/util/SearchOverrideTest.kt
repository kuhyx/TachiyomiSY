package exh.util

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import exh.GALLERY_URL
import exh.GalleryAdderHarness
import exh.MANGA_URL
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SearchOverrideTest {
    private val fallback = MangasPage(listOf(SManga.create().apply { url = "/fallback" }), true)

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun plainQueriesUseTheFallback() = runBlocking<Unit> {
        var called = 0
        val page = harness.source.urlImportSearchManga(harness.context, "solo") {
            called++
            fallback
        }
        page shouldBeSameInstanceAs fallback
        called shouldBe 1
    }

    @Test
    fun httpQueriesImportTheGallery() = runBlocking<Unit> {
        val page = harness.source.urlImportSearchManga(harness.context, GALLERY_URL) { error("not used") }
        page.mangas.single().url shouldBe MANGA_URL
        page.hasNextPage shouldBe false
        val plain = harness.source.urlImportSearchManga(harness.context, "http://plain.test/x") { error("not used") }
        plain.mangas.single().url shouldBe MANGA_URL
    }

    @Test
    fun failedImportsAreEmpty() = runBlocking<Unit> {
        every { harness.source.matchesUri(any()) } returns false
        val page = harness.source.urlImportSearchManga(harness.context, GALLERY_URL) { error("not used") }
        page.mangas.shouldBeEmpty()
    }

    // The gallery adder behind urlImportSearchManga is a process-wide lazy, so its collaborators
    // must be the same mocks for every test of this class.
    private companion object {
        val harness = GalleryAdderHarness()
    }
}
