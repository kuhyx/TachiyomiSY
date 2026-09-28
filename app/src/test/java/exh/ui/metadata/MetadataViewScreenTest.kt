package exh.ui.metadata

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class MetadataViewScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val getFlatMetadataById = mockk<GetFlatMetadataById>()
    private val getManga = mockk<GetManga>()
    private val sourceManager = mockk<SourceManager>()
    private lateinit var metadataSource: MetadataSource<EHentaiSearchMetadata, *>

    @Before
    fun setUp() {
        metadataSource = mockk { every { metaClass } returns EHentaiSearchMetadata::class }
        coEvery { getManga.await(5L) } returns Manga.create().copy(id = 5L, source = 8L, ogTitle = "Gallery")
        every { sourceManager.get(8L) } returns metadataSource
        stopKoin()
        startKoin {
            modules(
                module {
                    single { getFlatMetadataById }
                    single { sourceManager }
                    single { getManga }
                },
            )
        }
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        stopKoin()
    }

    private fun show() {
        compose.setContent { ScreenHost(MetadataViewScreen(mangaId = 5L, sourceId = 8L)) }
        compose.waitForLabel("Gallery")
    }

    @Test
    fun aSpinnerShowsWhileLoading() {
        val gate = CompletableDeferred<Unit>()
        coEvery { getFlatMetadataById.await(5L) } coAnswers {
            gate.await()
            null
        }
        show()
        compose.hasLabel("No results found") shouldBe false
        gate.complete(Unit)
        compose.waitForLabel("No results found")
    }

    @Test
    fun aMissingMangaHasNoTitle() {
        coEvery { getManga.await(5L) } returns null
        coEvery { getFlatMetadataById.await(5L) } returns null
        compose.setContent { ScreenHost(MetadataViewScreen(mangaId = 5L, sourceId = 8L)) }
        compose.waitForLabel("No results found")
        compose.hasLabel("Gallery") shouldBe false
    }

    @Test
    fun anUnknownSourceIsSaid() {
        every { sourceManager.get(8L) } returns null
        coEvery { getFlatMetadataById.await(5L) } returns null
        show()
        compose.waitForLabel("No source found")
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
    }

    @Test
    fun metadataRowsCopyOnHold() {
        val flat = EHentaiSearchMetadata().apply {
            mangaId = 5L
            gId = "123"
            gToken = "tok"
            title = "Main title"
        }.flatten()
        coEvery { getFlatMetadataById.await(5L) } returns flat
        show()
        compose.waitForLabel("Main title")
        compose.onNodeWithText("Main title").performClick()
        compose.onNodeWithText("Main title").performTouchInput { longClick() }
        compose.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.getItemAt(0)?.text.toString() shouldBe "Main title"
    }
}
