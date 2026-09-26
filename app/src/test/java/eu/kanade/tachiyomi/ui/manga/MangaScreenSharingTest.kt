package eu.kanade.tachiyomi.ui.manga

import android.content.ClipboardManager
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.feed.SourceFeedScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class MangaScreenSharingTest {
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val http = mockk<HttpSource> { every { getMangaUrl(any()) } returns "https://example.org/m/1" }

    private fun navigatorOver(vararg screens: Screen): Navigator = mockk(relaxed = true) {
        every { size } returns screens.size
        every { items } returns screens.toList()
    }

    @Test
    fun sharesTheUrl() {
        shareManga(activity, manga(), mockk<Source>())
        shadowOf(activity).nextStartedActivity.shouldBeNull()
        shareManga(activity, manga(), http)
        shadowOf(activity).nextStartedActivity.action shouldBe Intent.ACTION_CHOOSER
    }

    @Test
    fun shareFailureIsToasted() {
        val refusing = object : ContextWrapper(activity) {
            override fun startActivity(intent: Intent?) {
                error("no")
            }
        }
        shareManga(refusing, manga(), http)
        ShadowToast.getTextOfLatestToast() shouldBe "no"
    }

    @Test
    fun copiesTheUrl() {
        copyMangaUrl(activity, null, http)
        copyMangaUrl(activity, manga(), mockk<Source>())
        copyMangaUrl(activity, manga(), http)
        val clipboard = activity.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.getItemAt(0)?.text shouldBe "https://example.org/m/1"
    }

    @Test
    fun globalSearchPushes() = runBlocking {
        val navigator = navigatorOver()
        performSearch(navigator, "q", global = true)
        verify { navigator.push(any<GlobalSearchScreen>()) }
        performSearch(navigator, "q", global = false)
        verify(exactly = 0) { navigator.pop() }
    }

    @Test
    fun searchReachesBrowse() = runBlocking {
        val browse = BrowseSourceScreen(7L, null)
        val navigator = navigatorOver(browse, mockk<MangaScreen>())
        val sent = async { performSearch(navigator, "q", global = false) }
        BrowseSourceScreen.queryEvent.receive().txt shouldBe "q"
        sent.await()
        verify { navigator.pop() }
    }

    @Test
    fun searchReachesTheLibrary() = runBlocking {
        val navigator = navigatorOver(HomeScreen, mockk<MangaScreen>())
        withTimeoutOrNull(200L) { performSearch(navigator, "q", global = false) }
        verify { navigator.pop() }
    }

    @Test
    fun searchReplacesTheFeed() = runBlocking {
        val navigator = navigatorOver(SourceFeedScreen(7L), mockk<MangaScreen>())
        performSearch(navigator, "q", global = false)
        verify { navigator.replace(any<BrowseSourceScreen>()) }
        performSearch(navigatorOver(mockk<MangaScreen>(), mockk<MangaScreen>()), "q", global = false)
    }

    @Test
    fun genreSearchPrefersBrowse() = runBlocking {
        val browse = BrowseSourceScreen(7L, null)
        val navigator = navigatorOver(browse, mockk<MangaScreen>())
        val sent = async { performGenreSearch(navigator, "Action", http) }
        BrowseSourceScreen.queryEvent.receive().shouldBeInstanceOf<BrowseSourceScreen.SearchType.Genre>()
        sent.await()
        performGenreSearch(navigatorOver(), "Action", http)
    }

    @Test
    fun genreSearchFallsBack() = runBlocking {
        val browse = BrowseSourceScreen(7L, null)
        val navigator = navigatorOver(browse, mockk<MangaScreen>())
        val sent = async { performGenreSearch(navigator, "Action", mockk<Source>()) }
        BrowseSourceScreen.queryEvent.receive().shouldBeInstanceOf<BrowseSourceScreen.SearchType.Text>()
        sent.await()
        val feed = navigatorOver(SourceFeedScreen(7L), mockk<MangaScreen>())
        performGenreSearch(feed, "Action", http)
        verify { feed.replace(any<BrowseSourceScreen>()) }
    }
}
