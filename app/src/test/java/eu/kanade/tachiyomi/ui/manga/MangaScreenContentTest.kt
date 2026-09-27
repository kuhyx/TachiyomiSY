package eu.kanade.tachiyomi.ui.manga

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cafe.adriel.voyager.core.model.ScreenModelStore
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.readMember
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.poll
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The manga screen composed whole: waiting for sources and the model, the assist URL and EH redirects. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MangaScreenContentTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = MangaHarness()
    private val loaded = MutableStateFlow(true)

    @Before
    fun setUp() {
        resetUiDispatcher()
        every { harness.sourceManager.isInitialized } returns loaded
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(screen: MangaScreen = MangaScreen(1L)) = compose.setContent { ScreenHost(screen) }

    // Voyager keeps its cache internal; the model the screen made is read back through reflection.
    private fun model(): MangaScreenModel {
        val models = ScreenModelStore.readMember(ScreenModelStore::class, "screenModels") as Map<*, *>
        return models.values.filterIsInstance<MangaScreenModel>().last()
    }

    @Test
    fun waitsForSources() {
        loaded.value = false
        show()
        compose.waitForIdle()
        loaded.value = true
        compose.pollLabel("Needle")
    }

    @Test
    fun webSourceProvidesItsUrl() {
        val http = mockk<HttpSource>(relaxed = true) {
            every { id } returns 7L
            every { getMangaUrl(any()) } returns "https://example.org/m/1"
        }
        every { harness.sourceManager.getOrStub(any()) } returns http
        val screen = MangaScreen(1L)
        show(screen)
        compose.pollLabel("Needle")
        compose.poll({ "no assist url" }) { screen.onProvideAssistUrl() == "https://example.org/m/1" }
    }

    @Test
    fun redirectReplacesScreen() {
        show()
        compose.pollLabel("Needle")
        val model = model()
        // The collector runs on the main looper; emit from elsewhere so it can take the value.
        val sender = CoroutineScope(Dispatchers.Default).launch {
            model.redirectFlow.emit(MangaScreenModel.EXHRedirect(9L))
        }
        compose.pollLabel("opened:MangaScreen")
        sender.isCompleted shouldBe true
    }
}
