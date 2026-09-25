package exh.eh

import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ListenableWorker
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.debug.DebugToggles
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI

@RunWith(RobolectricTestRunner::class)
internal class EHentaiUpdateWorkerTest {
    private val harness = EhWorkerHarness()
    private val sdk = Build.VERSION.SDK_INT

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        DebugToggles.RESTRICT_EXH_GALLERY_UPDATE_CHECK_FREQUENCY.enabled = true
        harness.stop()
    }

    @Test
    fun wifiRequirementSkipsTheRun() = runBlocking<Unit> {
        harness.exhPreferences.exhAutoUpdateRequirements.set(setOf(DEVICE_ONLY_ON_WIFI))
        val worker = harness.worker()
        worker.requiresWifiConnection(harness.exhPreferences).shouldBeTrue()
        worker.doWork().shouldBeInstanceOf<ListenableWorker.Result.Success>()
        coVerify(exactly = 0) { harness.getExhFavoriteMangaWithMetadata.await() }
        harness.exhPreferences.exhAutoUpdateRequirements.set(emptySet())
        worker.requiresWifiConnection(harness.exhPreferences).shouldBeFalse()
    }

    @Test
    fun failuresStillSucceedForRetry() = runBlocking<Unit> {
        coEvery { harness.getExhFavoriteMangaWithMetadata.await() } throws IllegalStateException("db")
        harness.worker().doWork().shouldBeInstanceOf<ListenableWorker.Result.Success>()
        harness.exhPreferences.exhAutoUpdateStats.get() shouldBe ""
    }

    @Test
    fun emptyLibraryWritesStats() = runBlocking<Unit> {
        harness.worker().doWork().shouldBeInstanceOf<ListenableWorker.Result.Success>()
        val stats = Json.decodeFromString<EHentaiUpdaterStats>(harness.exhPreferences.exhAutoUpdateStats.get())
        stats.possibleUpdates shouldBe 0
        stats.updateCount shouldBe 0
    }

    @Test
    fun agedRecentAndUnknownAreSkipped() = runBlocking<Unit> {
        val aged = ehManga(1)
        val recent = ehManga(2)
        val unknown = ehManga(3)
        coEvery { harness.getExhFavoriteMangaWithMetadata.await() } returns listOf(aged, recent, unknown)
        coEvery { harness.getFlatMetadataById.await(1L) } returns ehMetadata(1, aged = true)
        coEvery { harness.getFlatMetadataById.await(2L) } returns
            ehMetadata(2, lastUpdateCheck = System.currentTimeMillis())
        harness.worker().doWork()
        Json.decodeFromString<EHentaiUpdaterStats>(harness.exhPreferences.exhAutoUpdateStats.get())
            .possibleUpdates shouldBe 0
        // Without the frequency restriction the recently checked gallery is due again.
        DebugToggles.RESTRICT_EXH_GALLERY_UPDATE_CHECK_FREQUENCY.enabled = false
        coEvery { harness.updateHelper.acceptRootAndDiscardOthers(any(), any()) } returns
            Triple(ChapterChain(recent, emptyList(), emptyList()), emptyList(), emptyList())
        coEvery { harness.getChaptersByMangaId.await(2L) } returns listOf(ehChapter(21, 2, "/s/a"))
        harness.stubRemote(recent, emptyList())
        harness.worker().doWork()
        Json.decodeFromString<EHentaiUpdaterStats>(harness.exhPreferences.exhAutoUpdateStats.get())
            .possibleUpdates shouldBe 1
    }

    @Test
    fun deadGalleriesAreAged() = runBlocking<Unit> {
        val manga = ehManga(4)
        coEvery { harness.getFlatMetadataById.await(4L) } returns ehMetadata(4)
        harness.stubRemoteFailure(manga, EHentai.GalleryNotFoundException(IllegalStateException()))
        val thrown = shouldThrow<GalleryNotUpdatedException> { harness.worker().updateEntryAndGetChapters(manga) }
        thrown.network.shouldBeFalse()
        coVerify(exactly = 1) { harness.insertFlatMetadata.await(match<EHentaiSearchMetadata> { it.aged }) }
        // A gallery whose metadata vanished meanwhile is simply not aged.
        coEvery { harness.getFlatMetadataById.await(4L) } returns null
        shouldThrow<GalleryNotUpdatedException> { harness.worker().updateEntryAndGetChapters(manga) }
        coVerify(exactly = 1) { harness.insertFlatMetadata.await(any<RaisedSearchMetadata>()) }
    }

    @Test
    fun otherFailuresAreNetwork() = runBlocking<Unit> {
        val manga = ehManga(5)
        harness.stubRemoteFailure(manga, IllegalStateException("offline"))
        val offline = shouldThrow<GalleryNotUpdatedException> { harness.worker().updateEntryAndGetChapters(manga) }
        offline.network.shouldBeTrue()
        val foreign = ehManga(6, source = 99L)
        val missing = shouldThrow<GalleryNotUpdatedException> { harness.worker().updateEntryAndGetChapters(foreign) }
        missing.network.shouldBeFalse()
        missing.cause?.message.orEmpty() shouldContain "Missing EH-based source"
    }

    @Test
    fun successReturnsNewAndCurrent() = runBlocking<Unit> {
        val manga = ehManga(7)
        val fresh = ehChapter(71, 7, "/s/new")
        harness.stubRemote(manga, listOf(fresh))
        coEvery { harness.getChaptersByMangaId.await(7L) } returns listOf(fresh, ehChapter(72, 7, "/s/old"))
        val (new, current) = harness.worker().updateEntryAndGetChapters(manga)
        new shouldBe listOf(fresh)
        current.size shouldBe 2
    }

    @Test
    fun foregroundInfoPerSdk() = runBlocking<Unit> {
        val worker = harness.worker()
        val info = worker.getForegroundInfo()
        info.notificationId shouldBe Notifications.ID_EHENTAI_PROGRESS
        info.foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        worker.getForegroundInfo().foregroundServiceType shouldBe 0
    }
}
