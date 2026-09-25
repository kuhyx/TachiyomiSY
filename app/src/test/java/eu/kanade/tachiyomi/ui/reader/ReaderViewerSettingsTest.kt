package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.cropBordersContinuousVertical
import eu.kanade.tachiyomi.ui.reader.setting.useAutoWebtoon
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReaderViewerSettingsTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
        harness.chapters(domainChapter(1L))
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun vmWithFlags(flags: Long): ReaderViewModel = harness.viewModel().also { vm ->
        vm.updateState { it.copy(manga = harness.manga.copy(viewerFlags = flags)) }
    }

    private fun named(name: String?) {
        val source = name?.let { n -> mockk<Source>().also { every { it.name } returns n } }
        every { harness.sourceManager.get(1L) } returns source
    }

    @Test
    fun readingModeFallsBack() {
        val settings = harness.viewModel().viewerSettings
        settings.getMangaReadingMode() shouldBe ReadingMode.RIGHT_TO_LEFT.flagValue
        vmWithFlags(ReadingMode.VERTICAL.flagValue.toLong()).viewerSettings.getMangaReadingMode() shouldBe
            ReadingMode.VERTICAL.flagValue
        vmWithFlags(0L).viewerSettings.getMangaReadingMode(resolveDefault = false) shouldBe 0
    }

    @Test
    fun autoWebtoonBySourceName() {
        named("Webtoons")
        vmWithFlags(0L).viewerSettings.getMangaReadingMode() shouldBe ReadingMode.WEBTOON.flagValue
        named("Plain")
        vmWithFlags(0L).viewerSettings.getMangaReadingMode() shouldBe ReadingMode.RIGHT_TO_LEFT.flagValue
        named(null)
        vmWithFlags(0L).viewerSettings.getMangaReadingMode() shouldBe ReadingMode.RIGHT_TO_LEFT.flagValue
        harness.readerPreferences.useAutoWebtoon.set(false)
        named("Webtoons")
        vmWithFlags(0L).viewerSettings.getMangaReadingMode() shouldBe ReadingMode.RIGHT_TO_LEFT.flagValue
    }

    @Test
    fun orientationFallsBack() {
        harness.viewModel().viewerSettings.getMangaOrientation() shouldBe ReaderOrientation.FREE.flagValue
        harness.viewModel().viewerSettings.getMangaOrientation(resolveDefault = false) shouldBe
            ReaderOrientation.FREE.flagValue
        val portrait = ReaderOrientation.PORTRAIT.flagValue.toLong()
        vmWithFlags(portrait).viewerSettings.getMangaOrientation() shouldBe ReaderOrientation.PORTRAIT.flagValue
        vmWithFlags(0L).viewerSettings.getMangaOrientation(resolveDefault = false) shouldBe 0
    }

    @Test
    fun cropBordersPerViewer() {
        vmWithFlags(ReadingMode.VERTICAL.flagValue.toLong()).viewerSettings.toggleCropBorders() shouldBe true
        harness.readerPreferences.cropBorders.get() shouldBe true
        vmWithFlags(ReadingMode.WEBTOON.flagValue.toLong()).viewerSettings.toggleCropBorders() shouldBe true
        harness.readerPreferences.cropBordersWebtoon.get() shouldBe true
        val continuous = ReadingMode.CONTINUOUS_VERTICAL.flagValue.toLong()
        vmWithFlags(continuous).viewerSettings.toggleCropBorders() shouldBe true
        harness.readerPreferences.cropBordersContinuousVertical.get() shouldBe true
    }

    @Test
    fun setReadingModeReloads() {
        harness.viewModel().viewerSettings.setMangaReadingMode(ReadingMode.WEBTOON)
        val vm = harness.loadedViewModel()
        vm.viewerSettings.setMangaReadingMode(ReadingMode.WEBTOON)
        coVerify { harness.setMangaViewerFlags.awaitSetReadingMode(10L, 4L) }
        val updated = harness.manga.copy(viewerFlags = 4L)
        coEvery { harness.getManga.await(10L) } returns updated
        val chapter = readerChapter(lastPageRead = 5)
        vm.updateState { it.copy(viewerChapters = ViewerChapters(chapter, null, null)) }
        runBlocking {
            val events = async(Dispatchers.Default) { vm.eventFlow.take(1).toList() }
            vm.viewerSettings.setMangaReadingMode(ReadingMode.WEBTOON)
            withTimeout(5_000) { events.await() } shouldBe listOf(Event.ReloadViewerChapters)
        }
        chapter.requestedPage shouldBe 5
        vm.manga shouldBe updated
    }

    @Test
    fun setOrientationReloads() {
        harness.viewModel().viewerSettings.setMangaOrientationType(ReaderOrientation.PORTRAIT)
        val vm = harness.loadedViewModel()
        vm.viewerSettings.setMangaOrientationType(ReaderOrientation.PORTRAIT)
        coVerify(timeout = 5_000) { harness.setMangaViewerFlags.awaitSetOrientation(10L, 0x10L) }
        coEvery { harness.getManga.await(10L) } returns harness.manga
        vm.updateState { it.copy(viewerChapters = ViewerChapters(readerChapter(), null, null)) }
        runBlocking {
            val events = async(Dispatchers.Default) { vm.eventFlow.take(2).toList() }
            vm.viewerSettings.setMangaOrientationType(ReaderOrientation.PORTRAIT)
            withTimeout(5_000) { events.await() } shouldBe listOf(
                Event.SetOrientation(ReaderOrientation.FREE.flagValue),
                Event.ReloadViewerChapters,
            )
        }
    }
}
