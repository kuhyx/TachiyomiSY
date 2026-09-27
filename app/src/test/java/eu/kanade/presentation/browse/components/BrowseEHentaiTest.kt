package eu.kanade.presentation.browse.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import eu.kanade.presentation.browse.BrowseEntry
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.browse.browseEntries
import eu.kanade.presentation.browse.browseManga
import eu.kanade.presentation.browse.browseSource
import eu.kanade.presentation.browse.placeholderPages
import eu.kanade.presentation.browse.rankedMeta
import eu.kanade.presentation.browse.resetUiDispatcher
import eu.kanade.presentation.browse.staticPages
import eu.kanade.presentation.browse.waitForTexts
import eu.kanade.presentation.util.PresentationKoin
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.Flow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.library.model.LibraryDisplayMode
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.concurrent.atomic.AtomicInteger

private fun galleryMeta(
    language: String? = "english",
    pages: Int? = 3,
    genre: String? = "doujinshi",
    rating: Double? = 3.7,
    posted: Long? = 1_000_000L,
    uploader: String? = "up",
) = EHentaiSearchMetadata().apply {
    language?.let { tags += RaisedTag(EHentaiSearchMetadata.EH_LANGUAGE_NAMESPACE, it, 0) }
    tags += RaisedTag("female", "x", 0)
    length = pages
    this.genre = genre
    averageRating = rating
    datePosted = posted
    this.uploader = uploader
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class BrowseEHentaiTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        resetUiDispatcher()
        koin.start()
    }

    @After
    fun tearDown() {
        koin.stop()
        unmockkAll()
    }

    private fun entries() = browseEntries(
        browseManga(1L, favorite = true) to galleryMeta(),
        browseManga(2L) to galleryMeta(language = null, pages = null, genre = "private", rating = null, posted = null),
        browseManga(3L) to galleryMeta(genre = null, uploader = null),
        browseManga(4L) to rankedMeta(rank = 1),
        browseManga(5L) to galleryMeta(posted = Long.MIN_VALUE),
        browseManga(6L) to galleryMeta(posted = Long.MAX_VALUE),
    )

    private fun show(pages: Flow<PagingData<BrowseEntry>>) {
        compose.setContent {
            MaterialTheme {
                BrowseSourceContent(
                    source = browseSource(EH_SOURCE_ID),
                    mangaList = pages.collectAsLazyPagingItems(),
                    columns = GridCells.Fixed(2),
                    ehentaiBrowseDisplayMode = true,
                    displayMode = LibraryDisplayMode.List,
                    snackbarHostState = SnackbarHostState(),
                    contentPadding = PaddingValues(),
                    onWebViewClick = null,
                    onHelpClick = null,
                    onLocalSourceHelpClick = null,
                    onMangaClick = { events += "click ${it.id}" },
                    onMangaLongClick = { events += "long ${it.id}" },
                )
            }
        }
        compose.waitForTexts { shows("Doujinshi") && shows("EN, 3 pages") }
    }

    private fun shows(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun galleryListWithLoadingEnds() {
        show(staticPages(entries(), prepend = LoadState.Loading, append = LoadState.Loading))
        compose.onNodeWithText("private").assertExists()
        compose.onNodeWithText("Title 1").performClick()
        compose.onNodeWithText("Title 2").performTouchInput { longClick() }
        compose.onNodeWithText("Title 4").assertDoesNotExist()
        events shouldContainExactly listOf("click 1", "long 2")
    }

    @Test
    fun galleryListPlaceholders() {
        show(placeholderPages(entries(), after = 2))
        compose.onAllNodes(hasText("up")).fetchSemanticsNodes().size shouldBe 4
    }

    @Test
    fun languageAndPageCounts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.languageAndPages(galleryMeta()) shouldBe "EN, 3 pages"
        context.languageAndPages(galleryMeta(language = null)) shouldBe "3 pages"
        context.languageAndPages(galleryMeta(pages = null)) shouldBe "EN"
        context.languageAndPages(galleryMeta(language = "klingon", pages = null)) shouldBe ""
    }

    @Test
    fun footerWithoutGenre() {
        val details = GalleryDetails(languageText = "L", datePosted = "D", genre = null, rating = 2.5f)
        compose.setContent {
            MaterialTheme {
                Column {
                    GalleryFooter(galleryMeta(genre = null), details)
                    GalleryFooter(galleryMeta(genre = "odd"), details)
                    GalleryCover(browseManga(9L, favorite = true), Color.Black)
                    BrowseSourceEHentaiListItem(browseManga(8L), metadata = galleryMeta())
                }
            }
        }
        compose.onNodeWithText("odd").assertExists()
        compose.onNodeWithText("In library").assertExists()
        compose.onNodeWithText("Title 8").performClick()
    }

    @Test
    fun unprintableDateStaysEmpty() {
        // A two-digit year field cannot print a four-digit year, so formatting the post date throws.
        val broken = DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 2).toFormatter()
        val asked = AtomicInteger(0)
        mockkObject(MetadataUtil)
        every { MetadataUtil.EX_DATE_FORMAT } answers {
            asked.incrementAndGet()
            broken
        }
        var details: GalleryDetails? = null
        compose.setContent { MaterialTheme { details = rememberGalleryDetails(galleryMeta()) } }
        compose.waitForTexts { asked.get() > 0 && details?.rating == 3.5f }
        compose.waitForIdle()
        details?.datePosted shouldBe ""
    }
}
