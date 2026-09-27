package eu.kanade.presentation.manga.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.components.SuggestionChipDefaults
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class ComponentDefaultsTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val manga = Manga.create().copy(id = 1L, ogTitle = "Needle")

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun coversAndListItems() {
        compose.setContent {
            MaterialTheme {
                Column {
                    MangaCover.Book(data = null)
                    MangaCover.Square(
                        data = manga,
                        modifier = Modifier,
                        contentDescription = "square",
                        shape = RectangleShape,
                        onClick = { events += "cover" },
                    )
                    BaseMangaListItem(manga = manga)
                }
            }
        }
        compose.onNodeWithContentDescription("square").performClick()
        compose.onNodeWithText("Needle").performClick()
        events shouldContainExactly listOf("cover")
    }

    @Test
    fun actionRowTracking() {
        compose.setContent {
            MaterialTheme {
                Column {
                    MangaActionRow(
                        favorite = true,
                        trackingCount = 2,
                        nextUpdate = null,
                        isUserIntervalMode = true,
                        onAddToLibraryClicked = { events += "favorite" },
                        onWebViewClicked = null,
                        onWebViewLongClicked = null,
                        onTrackingClicked = { events += "tracking" },
                        onEditIntervalClicked = null,
                        onEditCategory = null,
                        onMergeClicked = null,
                    )
                }
            }
        }
        compose.onNodeWithText("2 trackers").performClick()
        compose.onNodeWithText("N/A").performClick()
        events shouldContainExactly listOf("tracking")
    }

    @Test
    fun menusAndChipsWithDefaults() {
        compose.setContent {
            MaterialTheme {
                Column {
                    MangaBottomActionMenu(visible = true)
                    TagsChip(text = "plain", onClick = null)
                    TagsChip(
                        text = "bordered",
                        onClick = { events += "chip" },
                        modifier = Modifier,
                        border = SuggestionChipDefaults.suggestionChipBorder(borderWidth = 2.dp),
                        borderM3 = BorderStroke(1.dp, Color.Red),
                    )
                    TagsChip(text = "bare", onClick = { events += "bare" }, border = null, borderM3 = null)
                }
            }
        }
        compose.onNodeWithText("bordered").performClick()
        compose.onNodeWithText("bare").performClick()
        events shouldContainExactly listOf("chip", "bare")
    }

    @Test
    fun downloadIndicatorsDefaults() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row {
                        DownloadingIndicator(
                            enabled = true,
                            downloadState = Download.State.DOWNLOADING,
                            downloadProgressProvider = { 0 },
                            onClick = { events += "d $it" },
                        )
                        DownloadingIndicator(
                            enabled = true,
                            downloadState = Download.State.DOWNLOADING,
                            downloadProgressProvider = { 40 },
                            onClick = { events += "p $it" },
                        )
                        DownloadedIndicator(enabled = true, onClick = { events += "done $it" })
                        ErrorIndicator(enabled = true, onClick = { events += "error $it" })
                    }
                    Row {
                        progressRing(Download.State.DOWNLOADED, 0)
                        progressRing(Download.State.QUEUE, 0)
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Error").performClick()
        events shouldContainExactly listOf("error START")
    }
}
