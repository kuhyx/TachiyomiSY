package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreen
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.manga.model.Manga

/** Merging the smart-searched entry into the one the search started from, and what a failed merge shows. */
@RunWith(RobolectricTestRunner::class)
internal class MangaScreenMergeTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val navigator = mockk<Navigator>(relaxed = true)
    private val screen = MangaScreen(
        mangaId = 1L,
        fromSource = true,
        smartSearchConfig = SourcesScreen.SmartSearchConfig("t", 5L),
    )

    private fun merge(block: suspend () -> Manga) {
        screen.mergeWithAnother(navigator, app, manga()) { _, _ -> block() }
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun mergedEntryReplacesTheSearch() {
        merge { manga().copy(id = 9L) }
        verify(timeout = 5_000) { navigator.replace(any<MangaScreen>()) }
        verify { navigator.popUntil(any()) }
        ShadowToast.getTextOfLatestToast() shouldBe "Entry merged!"
    }

    @Test
    fun cancelledMergeStaysQuiet() {
        merge { throw CancellationException("left") }
        ShadowToast.shownToastCount() shouldBe 0
        verify(exactly = 0) { navigator.pop() }
    }

    @Test
    fun failedMergeSaysWhy() {
        merge { error("gone") }
        ShadowToast.getTextOfLatestToast() shouldBe "Failed to merge entry: gone"
        merge { throw IllegalStateException() }
        ShadowToast.getTextOfLatestToast() shouldBe "Failed to merge entry: "
    }
}
