package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.more.stats.data.StatsData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class StatsScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(tracked: Int, mean: Double, readMillis: Long = 0) {
        val state = StatsScreenState.Success(
            overview = StatsData.Overview(
                libraryMangaCount = 11,
                completedMangaCount = 2,
                totalReadDuration = readMillis,
            ),
            titles = StatsData.Titles(globalUpdateItemCount = 7, startedMangaCount = 5, localMangaCount = 1),
            chapters = StatsData.Chapters(totalChapterCount = 300, readChapterCount = 120, downloadCount = 40),
            trackers = StatsData.Trackers(trackedTitleCount = tracked, meanScore = mean, trackerCount = 3),
        )
        compose.setContent { MaterialTheme { StatsScreenContent(state = state, paddingValues = PaddingValues()) } }
        compose.waitForIdle()
    }

    @Test
    fun meanScoreShown() {
        show(tracked = 4, mean = 7.5, readMillis = 3_600_000)
        compose.onNodeWithText("11").assertExists()
        compose.onNodeWithText("7.50 ★").assertExists()
    }

    @Test
    fun untrackedIsNotApplicable() {
        show(tracked = 0, mean = 7.5)
        compose.onNodeWithText("N/A").assertExists()
    }

    @Test
    fun unscoredIsNotApplicable() {
        show(tracked = 2, mean = Double.NaN)
        compose.onNodeWithText("N/A").assertExists()
    }
}
