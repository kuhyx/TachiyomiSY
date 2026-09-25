package eu.kanade.tachiyomi.ui.manga

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import exh.pagepreview.PagePreviewScreen
import exh.recs.RecommendsScreen
import exh.ui.metadata.MetadataViewScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class MangaSyActionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = MangaHarness()
    private val host = MangaActionsHost(compose)

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L))
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun syScreensArePushed() {
        host.show(harness.loaded())
        val sy = host.actions.sy
        host.pushedBy { sy.onMetadataViewerClicked() }.shouldBeInstanceOf<MetadataViewScreen>()
        host.pushedBy { sy.onRecommendClicked() }.shouldBeInstanceOf<RecommendsScreen>()
        host.pushedBy { sy.merge.onMergeClicked() }.shouldBeInstanceOf<SourcesScreen>()
        host.pushedBy { sy.previews.onMorePreviewsClicked() }.shouldBeInstanceOf<PagePreviewScreen>()
        sy.previews.previewsRowCount shouldBe harness.uiPreferences.previewsRowCount.get()
    }

    @Test
    fun syDialogsOpen() {
        val model = harness.loaded()
        host.show(model)
        host.actions.sy.onEditInfoClicked()
        model.awaitSuccess().dialog.shouldBeInstanceOf<MangaScreenModel.Dialog.EditMangaInfo>()
        host.actions.sy.merge.onMergedSettingsClicked()
        host.actions.sy.previews.onOpenPagePreview(2)
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun mergeWithAnotherNeedsAnOrigin() {
        host.show(harness.loaded())
        host.actions.sy.merge.onMergeWithAnotherClicked()
        compose.waitForIdle()
        ShadowLooper.idleMainLooper()
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() > 0 }
    }

    @Test
    fun mergedWebViewOffersMembers() {
        val http = mockk<HttpSource>(relaxed = true) { every { id } returns 7L }
        every { harness.sourceManager.getOrStub(any()) } returns http
        val model = harness.loaded()
        model.updateSuccessState { it.copy(mergedData = mergedData(manga().copy(id = 4L))) }
        host.show(model)
        host.actions.header.onWebViewClicked!!()
        ShadowAlertDialog.getLatestAlertDialog().listView.adapter.count shouldBe 1
    }

    @Test
    fun loggedInTrackersOpenTheSheet() {
        val model = harness.loaded()
        model.updateSuccessState { it.copy(hasLoggedInTrackers = true) }
        host.show(model)
        host.actions.header.onTrackingClicked()
        model.awaitSuccess().dialog shouldBe MangaScreenModel.Dialog.TrackSheet
    }

    @Test
    fun webViewUsesTheUrl() {
        val http = mockk<HttpSource>(relaxed = true) {
            every { id } returns 7L
            every { getMangaUrl(any()) } returns "https://example.org"
        }
        every { harness.sourceManager.getOrStub(any()) } returns http
        host.show(harness.loaded())
        host.pushedBy { host.actions.header.onWebViewClicked!!() }.shouldBeInstanceOf<WebViewScreen>()
    }
}
