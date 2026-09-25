package exh

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class GalleryAdderImportTest {
    private val harness = GalleryAdderHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private suspend fun add(): GalleryAddEvent = harness.adder().addGallery(harness.context, GALLERY_URL)

    @Test
    fun chapterLinksResolveViaChapter() = runBlocking<Unit> {
        every { harness.source.mapUrlToChapterUrl(any()) } returns "/s/1/2"
        coEvery { harness.source.mapChapterUrlToMangaUrl(any()) } returns MANGA_URL
        val success = add().shouldBeInstanceOf<GalleryAddEvent.Success>()
        success.chapter shouldBeSameInstanceAs harness.chapter
        coVerify(exactly = 0) { harness.source.mapUrlToMangaUrl(any()) }
    }

    @Test
    fun chapterWithoutMangaFallsBack() = runBlocking<Unit> {
        every { harness.source.mapUrlToChapterUrl(any()) } returns "/s/1/2"
        coEvery { harness.getChapter.await(any<String>(), any()) } returns null
        val error = add().shouldBeInstanceOf<GalleryAddEvent.Fail.Error>()
        error.logMessage shouldContain GALLERY_URL
        coVerify(exactly = 1) { harness.source.mapUrlToMangaUrl(any()) }
    }

    @Test
    fun mappingFailuresAreLoggedAsNull() = runBlocking<Unit> {
        every { harness.source.mapUrlToChapterUrl(any()) } throws IllegalStateException("map")
        add().shouldBeInstanceOf<GalleryAddEvent.Success>().chapter.shouldBeNull()
        every { harness.source.mapUrlToChapterUrl(any()) } returns "/s/1/2"
        every { harness.source.cleanChapterUrl(any()) } throws IllegalStateException("clean")
        add().shouldBeInstanceOf<GalleryAddEvent.Success>().chapter.shouldBeNull()
    }

    @Test
    fun unmappableUrlIsUnknownType() = runBlocking<Unit> {
        coEvery { harness.source.mapUrlToMangaUrl(any()) } returns null
        add().shouldBeInstanceOf<GalleryAddEvent.Fail.UnknownType>()
        coEvery { harness.source.mapUrlToMangaUrl(any()) } throws IllegalStateException("map")
        add().shouldBeInstanceOf<GalleryAddEvent.Fail.UnknownType>()
        coEvery { harness.source.mapUrlToMangaUrl(any()) } returns MANGA_URL
        every { harness.source.cleanMangaUrl(any()) } throws IllegalStateException("clean")
        add().shouldBeInstanceOf<GalleryAddEvent.Fail.UnknownType>()
    }

    @Test
    fun unknownMangaIsInsertedFirst() = runBlocking<Unit> {
        coEvery { harness.getManga.await(any<String>(), any()) } returns null
        add().shouldBeInstanceOf<GalleryAddEvent.Success>()
        coVerify(exactly = 1) { harness.networkToLocalManga(any<tachiyomi.domain.manga.model.Manga>()) }
    }

    @Test
    fun remoteFailureIsReported() = runBlocking<Unit> {
        coEvery {
            harness.updateMangaFromRemote(
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.failure(IllegalStateException("remote"))
        val error = harness.adder().addGallery(harness.context, GALLERY_URL, retry = 2)
            .shouldBeInstanceOf<GalleryAddEvent.Fail.Error>()
        error.logMessage shouldBe "remote (Gallery: $GALLERY_URL)"
    }
}
