package eu.kanade.presentation.manga.components

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.manga.EditCoverAction
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.atomic.AtomicInteger

@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
internal class MangaCoverDialogTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    // Counted on the image loader's thread, read on the test thread.
    private val fetched = AtomicInteger(0)

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
        koin.stop()
    }

    private fun loadWith(image: Image) {
        val loader = ImageLoader.Builder(ApplicationProvider.getApplicationContext()).components {
            add(
                Fetcher.Factory<Manga> { _, _, _ ->
                    Fetcher {
                        fetched.incrementAndGet()
                        ImageFetchResult(image, isSampled = false, dataSource = DataSource.MEMORY)
                    }
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    private fun show(custom: Boolean, editable: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                MangaCoverDialog(
                    manga = Manga.create().copy(id = 1L, ogTitle = "Needle"),
                    isCustomCover = custom,
                    snackbarHostState = SnackbarHostState(),
                    onShareClick = { events += "share" },
                    onSaveClick = { events += "save" },
                    onEditClick = { action: EditCoverAction -> events += "edit $action" }.takeIf { editable },
                    onDismissRequest = { events += "dismiss" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun sourceCoverActions() {
        loadWith(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImage())
        show(custom = false)
        eventually { fetched.get() > 0 }
        listOf("Share", "Save", "Edit cover", "Close").forEach {
            compose.onNodeWithContentDescription(it).performClick()
        }
        events shouldContainExactly listOf("share", "save", "edit EDIT", "dismiss")
    }

    @Test
    fun customCoverMenu() {
        loadWith(ColorDrawable(0).asImage())
        show(custom = true)
        eventually { fetched.get() > 0 }
        compose.onNodeWithContentDescription("Edit cover").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithContentDescription("Edit cover").performClick()
        compose.onNodeWithText("Delete").performClick()
        events shouldContainExactly listOf("edit EDIT", "edit DELETE")
    }

    @Test
    fun notEditable() {
        show(custom = true, editable = false)
        compose.onNodeWithContentDescription("Edit cover").assertDoesNotExist()
        compose.onNodeWithContentDescription("Save").assertExists()
    }
}
