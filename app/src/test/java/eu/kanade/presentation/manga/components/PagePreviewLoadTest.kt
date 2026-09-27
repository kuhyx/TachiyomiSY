package eu.kanade.presentation.manga.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.presentation.browse.UiDispatcherReset
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

/** Page previews through a fake image loader: page 3 loads, pages 1 and 2 stay loading with and without progress. */
@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PagePreviewLoadTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    // Counted on the image loader's thread, read on the test thread.
    private val loaded = AtomicInteger(0)

    @Before
    fun setUp() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val loader = ImageLoader.Builder(ApplicationProvider.getApplicationContext()).components {
            add(
                Fetcher.Factory<PagePreview> { page, _, _ ->
                    Fetcher {
                        if (page.index < 3) awaitCancellation()
                        loaded.incrementAndGet()
                        ImageFetchResult(bitmap.asImage(), isSampled = false, dataSource = DataSource.MEMORY)
                    }
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @After
    fun tearDown() = SingletonImageLoader.reset()

    @Test
    fun loadingAndLoadedPages() {
        val pages = (1..3).map { PagePreview(index = it, imageUrl = "https://h/$it", source = 1L) }
        pages[0].toPagePreviewInfo().update(bytesRead = 50L, contentLength = 100L, done = false)
        compose.setContent {
            MaterialTheme {
                Row {
                    pages.forEach { PagePreview(modifier = Modifier.weight(1f), page = it, onOpenPage = {}) }
                }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000L) { loaded.get() > 0 }
        // The fetch counts before the decode, which finishes on another thread.
        repeat(times = 20) {
            Thread.sleep(50L)
            compose.waitForIdle()
        }
        compose.onAllNodes(hasText("3")).fetchSemanticsNodes().size shouldBe 1
    }
}
