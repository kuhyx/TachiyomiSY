package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.eh.ChapterChain
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EH_SOURCE_ID
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
internal class MangaScreenObserversTest {
    private val harness = MangaHarness()

    // Calls to acceptRootAndDiscardOthers; waiting on it idles the main looper the observer chain runs on.
    private val accepted = AtomicInteger()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun accept(root: Manga) {
        coEvery { harness.updateHelper.acceptRootAndDiscardOthers(any(), any()) } coAnswers {
            accepted.incrementAndGet()
            delay(300L)
            Triple(ChapterChain(root, emptyList(), emptyList()), emptyList(), emptyList())
        }
    }

    @Test
    fun ehentaiRedirectsToTheRoot() {
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(1L))
        accept(manga(favorite = true).copy(id = 4L))
        val model = harness.model()
        // The chain runs through the main looper, so wait with eventually instead of blocking it.
        val redirect = CoroutineScope(Dispatchers.Default)
            .async(start = CoroutineStart.UNDISPATCHED) { model.redirectFlow.first() }
        eventually { redirect.isCompleted }
        runBlocking { redirect.await() } shouldBe MangaScreenModel.EXHRedirect(4L)
    }

    @Test
    fun acceptedRootStaysPut() {
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(1L))
        accept(manga(favorite = true))
        harness.loaded()
        eventually { accepted.get() == 1 }
        coVerify { harness.updateHelper.acceptRootAndDiscardOthers(EH_SOURCE_ID, any()) }
        accept(manga().copy(id = 4L))
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(2L))
        eventually { accepted.get() == 2 }
        coVerify(exactly = 2) { harness.updateHelper.acceptRootAndDiscardOthers(any(), any()) }
    }

    @Test
    fun redirectErrorsAreLogged() {
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(1L))
        coEvery { harness.updateHelper.acceptRootAndDiscardOthers(any(), any()) } answers {
            accepted.incrementAndGet()
            error("no chain")
        }
        val model = harness.loaded()
        model.awaitSuccess().chapters.size shouldBe 1
        eventually { accepted.get() == 1 }
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to emptyList()
        model.awaitSuccess { it.chapters.isEmpty() }
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        model.awaitSuccess { it.manga.source == 7L && it.chapters.size == 1 }
        coVerify(exactly = 1) { harness.updateHelper.acceptRootAndDiscardOthers(any(), any()) }
    }

    @Test
    fun mergedEntryUsesMergedChapters() {
        val merged = manga(source = MERGED_SOURCE_ID, favorite = true)
        harness.mangaFlow.value = merged to listOf(chapter(1L))
        coEvery { harness.getMergedChapters.subscribe(any(), any(), any()) } returns
            MutableStateFlow(listOf(chapter(2L), chapter(3L)))
        coEvery { harness.getMergedChapters.await(any(), any(), any()) } returns listOf(chapter(2L))
        val model = harness.loaded()
        val state = model.awaitSuccess { it.chapters.size == 2 && it.availableScanlators.contains("merge") }
        state.chapters.map { it.id } shouldBe listOf(2L, 3L)
        coVerify { harness.getAvailableScanlators.awaitMerge(1L) }
    }

    @Test
    fun mergedDataFollowsTheTables() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        val members = MutableStateFlow(emptyList<Manga>())
        coEvery { harness.getMergedManga.subscribe(1L) } returns members
        val model = harness.loaded()
        model.awaitSuccess().mergedData shouldBe null
        members.value = listOf(manga().copy(id = 5L, source = 8L))
        val data = model.awaitSuccess { it.mergedData != null }.mergedData!!
        data.manga.keys shouldBe setOf(5L)
    }

    @Test
    fun metadataNeedsAMetadataSource() {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        model.raiseMetadata(null, harness.source) shouldBe null
        val metaSource = mockk<MetadataSource<*, *>>(
            moreInterfaces = arrayOf(Source::class),
        )
        every { metaSource.metaClass } returns EHentaiSearchMetadata::class
        model.raiseMetadata(null, metaSource as Source) shouldBe null
    }
}
