package eu.kanade.tachiyomi.ui.browse.migration.manga

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import mihon.feature.migration.config.MigrationConfigScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

private const val INTERNAL_ERROR = "InternalError: Check crash logs for further information"

/** The list of a source's favorites to pick for migration. */
@RunWith(RobolectricTestRunner::class)
internal class MigrateMangaScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val koin = BrowseKoin()
    private val source = mockk<Source> {
        every { id } returns 4L
        every { name } returns "Old site"
    }
    private val sources = mockk<SourceManager> { every { getOrStub(4L) } returns source }
    private val getFavorites = mockk<GetFavorites>()
    private val host = ScreenHost(MigrateMangaScreen(4L))

    private fun favorite(id: Long, title: String) = Manga.create().copy(id = id, ogTitle = title, source = 4L)

    @Before
    fun setUp() = koin.start(
        module {
            single { sources }
            single { getFavorites }
        },
    )

    @After
    fun tearDown() = koin.stop()

    private fun show(vararg manga: Manga) {
        every { getFavorites.subscribe(4L) } returns MutableStateFlow(manga.toList())
        host.show(compose)
    }

    @Test
    fun selectionContinuesToConfig() {
        show(favorite(2L, "beta"), favorite(1L, "Alpha"))
        compose.pollLabel("Alpha")
        compose.clickLabel("Alpha")
        compose.clickLabel("Continue")
        host.top.shouldBeInstanceOf<MigrationConfigScreen>()
    }

    @Test
    fun upClearsSelectionFirst() {
        show(favorite(1L, "Alpha"))
        compose.pollLabel("Alpha")
        compose.clickLabel("Alpha")
        compose.clickLabel("Navigate up")
        host.top.shouldBeInstanceOf<MigrateMangaScreen>()
        compose.clickLabel("Navigate up")
        host.top.shouldBeInstanceOf<BlankScreen>()
    }

    @Test
    fun backClearsSelection() {
        show(favorite(1L, "Alpha"))
        compose.pollLabel("Alpha")
        compose.clickLabel("Alpha")
        compose.pollLabel("Continue")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        host.top.shouldBeInstanceOf<MigrateMangaScreen>()
    }

    @Test
    fun emptyListSaysSo() {
        show()
        compose.pollLabel("Well, this is awkward")
    }

    @Test
    fun waitsForFavorites() {
        every { getFavorites.subscribe(4L) } returns MutableSharedFlow()
        host.show(compose)
        compose.labelShown("Old site") shouldBe false
    }

    // A failure on the first emission arrives while the screen still shows its spinner.
    @Test
    fun earlyFailureToasts() {
        every { getFavorites.subscribe(4L) } returns flow { error("offline") }
        host.show(compose)
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.getTextOfLatestToast() == INTERNAL_ERROR }
        compose.pollLabel("Well, this is awkward")
    }

    @Test
    fun lateFailureToasts() {
        every { getFavorites.subscribe(4L) } returns flow {
            emit(listOf(favorite(1L, "Alpha")))
            error("offline")
        }
        host.show(compose)
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.getTextOfLatestToast() == INTERNAL_ERROR }
    }
}
