package eu.kanade.tachiyomi.ui.manga

import androidx.lifecycle.Lifecycle
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.ui.reader.setting.preserveReadingPosition
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The first load with every overflow flag, EH reading progress, failed refreshes and the observers' end. */
@RunWith(RobolectricTestRunner::class)
internal class MangaLoadEdgesTest {
    private val harness = MangaHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
    }

    @After
    fun tearDown() = harness.stop()

    private fun refreshFails() {
        coEvery {
            harness.updateMangaFromRemote(
                source = any(),
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.failure(IllegalStateException("offline"))
    }

    @Test
    fun overflowFlagsReachTheState() {
        harness.uiPreferences.recommendsInOverflow.set(true)
        harness.uiPreferences.mergeInOverflow.set(true)
        val state = harness.model(fromSource = true, smartSearched = true).awaitSuccess { !it.isRefreshingData }
        state.showRecommendationsInOverflow shouldBe true
        state.showMergeInOverflow shouldBe true
        state.showMergeWithAnother shouldBe true
        state.isFromSource shouldBe true
    }

    @Test
    fun plainEntriesHideProgress() {
        harness.readerPreferences.preserveReadingPosition.set(true)
        harness.loaded().awaitSuccess().alwaysShowReadingProgress shouldBe false
    }

    @Test
    fun ehEntriesShowReadingProgress() {
        harness.readerPreferences.preserveReadingPosition.set(true)
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(1L))
        harness.loaded().awaitSuccess().alwaysShowReadingProgress shouldBe true
    }

    @Test
    fun failedRefreshIsShownOnce() {
        refreshFails()
        harness.mangaFlow.value = manga(favorite = true) to emptyList()
        val model = harness.loaded()
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        eventually { model.snackbarHostState.currentSnackbarData == null }
    }

    @Test
    fun refreshWhileLoadingIsSkipped() {
        val model = harness.loading()
        runBlocking { model.fetchAllFromSource(manualFetch = true, fetchDetails = true, fetchChapters = true) }
        model.successState shouldBe null
    }

    @Test
    fun storedMetadataIsRaised() {
        val model = harness.loaded()
        val metaSource = mockk<MetadataSource<*, *>>(moreInterfaces = arrayOf(Source::class))
        every { metaSource.metaClass } returns EHentaiSearchMetadata::class
        val flat = EHentaiSearchMetadata().apply { mangaId = 1L }.flatten()
        model.raiseMetadata(flat, metaSource as Source).shouldNotBeNull()
    }

    @Test
    fun destroyedScreenStopsObserving() {
        val model = harness.loaded()
        harness.awaitObserver(model)
        eventually { harness.statuses.subscriptionCount.value > 0 }
        harness.lifecycle.currentState = Lifecycle.State.DESTROYED
        eventually { harness.statuses.subscriptionCount.value == 0 }
        eventually { harness.mangaFlow.subscriptionCount.value == 0 }
    }
}
