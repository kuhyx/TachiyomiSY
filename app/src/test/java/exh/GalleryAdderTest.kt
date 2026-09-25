package exh

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.all.EHentai
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class GalleryAdderTest {
    private val harness = GalleryAdderHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun pickSourceFilters() {
        val german = importableSource(sourceId = 3L, sourceLang = "de")
        val disabled = importableSource(sourceId = 2L)
        val noMatch = importableSource(sourceId = 4L).also { every { it.matchesUri(any()) } returns false }
        val throwing = importableSource(sourceId = 6L).also {
            every { it.matchesUri(any()) } throws IllegalStateException()
        }
        val plain = mockk<Source>()
        every { harness.sourceManager.getVisibleSources() } returns
            listOf(german, disabled, noMatch, throwing, plain, harness.source)
        harness.adder().pickSource(GALLERY_URL) shouldContainExactly listOf(harness.source)
    }

    @Test
    fun defaultsResolveFromKoin() {
        GalleryAdder().pickSource(GALLERY_URL) shouldContainExactly listOf(harness.source)
    }

    @Test
    fun addsGalleryFromMatchingSource() = runBlocking<Unit> {
        val event = harness.adder().addGallery(harness.context, GALLERY_URL)
        val success = event.shouldBeInstanceOf<GalleryAddEvent.Success>()
        success.manga shouldBeSameInstanceAs harness.manga
        success.chapter.shouldBeNull()
        success.galleryTitle shouldBe "Title"
        success.logMessage shouldContain "Title"
    }

    @Test
    fun noSourceClaimsTheUrl() = runBlocking<Unit> {
        val german = importableSource(sourceId = 3L, sourceLang = "de")
        val disabled = importableSource(sourceId = 2L)
        val noMatch = importableSource(sourceId = 4L).also { every { it.matchesUri(any()) } returns false }
        every { harness.sourceManager.getVisibleSources() } returns listOf(german, disabled, noMatch)
        val event = harness.adder().addGallery(harness.context, GALLERY_URL)
        event.shouldBeInstanceOf<GalleryAddEvent.Fail.UnknownSource>().galleryUrl shouldBe GALLERY_URL
    }

    @Test
    fun forcedSourceMustMatch() = runBlocking<Unit> {
        val adder = harness.adder()
        val other = importableSource(sourceId = 7L).also { every { it.matchesUri(any()) } returns false }
        adder.addGallery(harness.context, GALLERY_URL, forceSource = other)
            .shouldBeInstanceOf<GalleryAddEvent.Fail.UnknownSource>()
        every { other.matchesUri(any()) } throws IllegalStateException("bad uri")
        adder.addGallery(harness.context, GALLERY_URL, forceSource = other)
            .shouldBeInstanceOf<GalleryAddEvent.Fail.UnknownType>()
        val favourite = adder.addGallery(harness.context, GALLERY_URL, forceSource = harness.source, fav = true)
        favourite.shouldBeInstanceOf<GalleryAddEvent.Success>().manga.favorite shouldBe true
    }

    @Test
    fun galleryNotFoundIsReported() = runBlocking<Unit> {
        coEvery {
            harness.updateMangaFromRemote(
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.failure(EHentai.GalleryNotFoundException(IllegalStateException()))
        val event = harness.adder().addGallery(harness.context, GALLERY_URL)
        event.shouldBeInstanceOf<GalleryAddEvent.Fail.NotFound>().logMessage shouldContain GALLERY_URL
    }

    @Test
    fun otherFailuresBecomeErrors() = runBlocking<Unit> {
        every { harness.source.matchesUri(any()) } throws IllegalStateException("boom")
        val adder = harness.adder()
        adder.addGallery(harness.context, "bad url").shouldBeInstanceOf<GalleryAddEvent.Fail.UnknownSource>()
        every { harness.sourceManager.getVisibleSources() } throws IllegalStateException("boom")
        val error = adder.addGallery(harness.context, GALLERY_URL).shouldBeInstanceOf<GalleryAddEvent.Fail.Error>()
        error.logMessage shouldBe "boom (Gallery: $GALLERY_URL)"
        every { harness.sourceManager.getVisibleSources() } throws IllegalStateException()
        adder.addGallery(harness.context, GALLERY_URL).shouldBeInstanceOf<GalleryAddEvent.Fail.Error>()
            .logMessage shouldBe "Unknown error! (Gallery: $GALLERY_URL)"
    }

    @Test
    fun retryStopsAtFirstSuccess() {
        val adder = harness.adder()
        var calls = 0
        adder.retry(3) { calls++ } shouldBe 0
        calls shouldBe 1
    }

    /** An earlier failure is remembered even when a later attempt succeeds. */
    @Test
    fun retryKeepsEarlierFailure() {
        var calls = 0
        val thrown = runCatching {
            harness.adder().retry(3) {
                calls++
                if (calls < 2) error("again") else "ok"
            }
        }.exceptionOrNull()
        thrown.shouldNotBeNull().message shouldBe "again"
        calls shouldBe 2
    }

    @Test
    fun retryRethrowsAfterLastFailure() {
        val adder = harness.adder()
        val thrown = runCatching { adder.retry(2) { error("always") } }.exceptionOrNull()
        thrown.shouldNotBeNull().message shouldBe "always"
        val notFound = EHentai.GalleryNotFoundException(IllegalStateException())
        runCatching { adder.retry(2) { throw notFound } }.exceptionOrNull() shouldBeSameInstanceAs notFound
    }
}
