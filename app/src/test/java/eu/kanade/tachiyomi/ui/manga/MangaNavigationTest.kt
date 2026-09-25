package eu.kanade.tachiyomi.ui.manga

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import exh.pagepreview.PagePreviewScreen
import exh.recs.RecommendsScreen
import exh.source.MERGED_SOURCE_ID
import exh.ui.metadata.MetadataViewScreen
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class MangaNavigationTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val navigator = mockk<Navigator>(relaxed = true)
    private val http = mockk<HttpSource> {
        every { id } returns 7L
        every { getMangaUrl(any()) } returns "https://example.org/m/1"
    }
    private val sourceManager = mockk<SourceManager> { every { getOrStub(any()) } returns http }

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { sourceManager }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun pushed(): Any? {
        val screens = mutableListOf<Screen>()
        verify { navigator.push(capture(screens)) }
        return screens.last()
    }

    @Test
    fun chaptersOpenTheReader() {
        continueReading(activity, null)
        shadowOf(activity).nextStartedActivity.shouldBeNull()
        continueReading(activity, chapter(3L))
        shadowOf(activity).nextStartedActivity.component?.className shouldBe ReaderActivity::class.java.name
        openPagePreview(activity, null, page = 2)
        shadowOf(activity).nextStartedActivity.shouldBeNull()
        openPagePreview(activity, chapter(3L), page = 2)
        shadowOf(activity).nextStartedActivity.extras?.getInt("page") shouldBe 2
    }

    @Test
    fun webViewNeedsAUrl() {
        openMangaInWebView(navigator, null, http)
        verify(exactly = 0) { navigator.push(any<Screen>()) }
        openMangaInWebView(navigator, manga(), http)
        pushed().shouldBeInstanceOf<WebViewScreen>()
    }

    @Test
    fun screensArePushed() {
        openMetadataViewer(navigator, manga())
        pushed().shouldBeInstanceOf<MetadataViewScreen>()
        openMorePagePreviews(navigator, manga())
        pushed().shouldBeInstanceOf<PagePreviewScreen>()
        openSmartSearch(navigator, manga())
        pushed().shouldBeInstanceOf<SourcesScreen>()
        openRecommends(navigator, null, manga())
        openRecommends(navigator, http, manga())
        pushed().shouldBeInstanceOf<RecommendsScreen>()
    }

    @Test
    fun mergedWebViewPicksAMember() {
        val themed = ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_MaterialComponents)
        val member = manga().copy(id = 4L, source = 7L)
        val self = manga().copy(id = 5L, source = MERGED_SOURCE_ID)
        openMergedMangaWebview(themed, navigator, mergedData(member, self))
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.listView.adapter.count shouldBe 1
        dialog.listView.performItemClick(null, 0, 0L)
        pushed().shouldBeInstanceOf<WebViewScreen>()
    }

    @Test
    fun urlNeedsAnHttpSource() {
        getMangaUrl(null, http).shouldBeNull()
        getMangaUrl(manga(), mockk<Source>()).shouldBeNull()
        getMangaUrl(manga(), http) shouldBe "https://example.org/m/1"
        val failing = mockk<HttpSource> { every { getMangaUrl(any()) } throws IllegalStateException() }
        getMangaUrl(manga(), failing).shouldBeNull()
    }
}
