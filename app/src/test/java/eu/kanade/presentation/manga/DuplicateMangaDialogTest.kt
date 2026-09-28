package eu.kanade.presentation.manga

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1200dp-h2000dp")
internal class DuplicateMangaDialogTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val sourceManager = mockk<SourceManager> {
        every { getOrStub(any()) } answers {
            val id = firstArg<Long>()
            if (id == 2L) StubSource(id = 2L, lang = "en", name = "Gone") else plainSource(id)
        }
    }
    private var duplicates by mutableStateOf(emptyList<MangaWithChapterCount>())
    private var withModifier by mutableStateOf(false)

    @Before
    fun setUp() = koin.start(module { single { sourceManager } })

    @After
    fun tearDown() = koin.stop()

    private fun duplicate(id: Long, author: String?, artist: String?, source: Long = 7L) = MangaWithChapterCount(
        manga = Manga.create()
            .copy(id = id, source = source, ogTitle = "Title $id", ogAuthor = author, ogArtist = artist),
        chapterCount = id,
    )

    private fun show() {
        compose.setContent {
            MaterialTheme {
                val onOpen = { manga: Manga -> events += "open ${manga.id}" }
                val onMigrate = { manga: Manga -> events += "migrate ${manga.id}" }
                if (withModifier) {
                    DuplicateMangaDialog(
                        duplicates = duplicates,
                        onDismissRequest = { events += "dismiss" },
                        onConfirm = { events += "confirm" },
                        onOpenManga = onOpen,
                        onMigrate = onMigrate,
                        modifier = Modifier,
                    )
                } else {
                    DuplicateMangaDialog(
                        duplicates = duplicates,
                        onDismissRequest = { events += "dismiss" },
                        onConfirm = { events += "confirm" },
                        onOpenManga = onOpen,
                        onMigrate = onMigrate,
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun cardsForwardClicks() {
        duplicates = listOf(
            duplicate(1L, author = "Ann", artist = "Bob"),
            duplicate(2L, author = "Same", artist = "Same", source = 2L),
            duplicate(3L, author = " ", artist = null),
        )
        show()
        compose.onNodeWithText("Bob").assertExists()
        compose.onNodeWithText("Gone").assertExists()
        compose.onNodeWithText("Title 1").performClick()
        compose.onNodeWithText("Title 3").performTouchInput { longClick() }
        compose.onNodeWithText("Add anyway").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss", "migrate 1", "open 3", "dismiss", "confirm", "dismiss")
    }

    @Test
    fun emptyWithModifier() {
        withModifier = true
        show()
        compose.onNodeWithText("Possible duplicates").assertExists()
        // A blank artist is no artist, whatever the author.
        duplicates = listOf(duplicate(4L, author = null, artist = "Art"), duplicate(5L, author = "Ann", artist = " "))
        withModifier = false
        compose.waitForIdle()
        compose.onNodeWithText("Art").assertExists()
    }
}
