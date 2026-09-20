package eu.kanade.presentation.track

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Bounds of the random preview data.
private const val PREVIEW_RESULTS = 30
private const val TITLE_WORDS_MAX = 10
private const val CHAPTERS_READ_MAX = 100
private const val TOTAL_CHAPTERS_MIN = 100L
private const val TOTAL_CHAPTERS_MAX = 1000L
private const val SCORE_MAX = 10
private const val DAYS_PER_YEAR = 365L
private const val SUMMARY_WORDS_MAX = 40
private const val NAMES_MAX = 3
private const val NAME_WORDS_MIN = 3
private const val NAME_WORDS_MAX = 5

internal class TrackerSearchPreviewProvider : PreviewParameterProvider<@Composable () -> Unit> {
    private val fullPageWithSecondSelected = @Composable {
        val items = someTrackSearches().take(PREVIEW_RESULTS).toList()
        TrackerSearch(
            state = TextFieldState(initialText = "search text"),
            onDispatchQuery = {},
            queryResult = Result.success(items),
            selected = items[1],
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = false,
        )
    }
    private val fullPageWithoutSelected = @Composable {
        TrackerSearch(
            state = TextFieldState(),
            onDispatchQuery = {},
            queryResult = Result.success(someTrackSearches().take(PREVIEW_RESULTS).toList()),
            selected = null,
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = false,
        )
    }
    private val loading = @Composable {
        TrackerSearch(
            state = TextFieldState(),
            onDispatchQuery = {},
            queryResult = null,
            selected = null,
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = false,
        )
    }
    private val fullPageWithPrivateTracking = @Composable {
        val items = someTrackSearches().take(PREVIEW_RESULTS).toList()
        TrackerSearch(
            state = TextFieldState(initialText = "search text"),
            onDispatchQuery = {},
            queryResult = Result.success(items),
            selected = items[1],
            onSelectedChange = {},
            onConfirmSelection = {},
            onDismissRequest = {},
            supportsPrivateTracking = true,
        )
    }
    override val values: Sequence<@Composable () -> Unit> = sequenceOf(
        fullPageWithSecondSelected,
        fullPageWithoutSelected,
        loading,
        fullPageWithPrivateTracking,
    )

    private fun someTrackSearches(): Sequence<TrackSearch> = sequence {
        while (true) {
            yield(randTrackSearch())
        }
    }

    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun randTrackSearch() = TrackSearch().let {
        it.id = Random.nextLong()
        it.mangaId = Random.nextLong()
        it.trackerId = Random.nextLong()
        it.remoteId = Random.nextLong()
        it.libraryId = Random.nextLong()
        it.title = lorem((1..TITLE_WORDS_MAX).random()).joinToString()
        it.lastChapterRead = (0..CHAPTERS_READ_MAX).random().toDouble()
        it.totalChapters = (TOTAL_CHAPTERS_MIN..TOTAL_CHAPTERS_MAX).random()
        it.score = (0..SCORE_MAX).random().toDouble()
        it.status = Random.nextLong()
        it.startedReadingDate = 0L
        it.finishedReadingDate = 0L
        it.trackingUrl = "https://example.com/tracker-example"
        it.coverUrl = "https://example.com/cover.png"
        it.startDate = formatter.format(Date.from(Instant.now().minus((1L..DAYS_PER_YEAR).random(), ChronoUnit.DAYS)))
        it.summary = lorem((0..SUMMARY_WORDS_MAX).random()).joinToString()
        it.publishingStatus = if (Random.nextBoolean()) "Finished" else ""
        it.publishingType = if (Random.nextBoolean()) "Oneshot" else ""
        it.artists = randomNames()
        it.authors = randomNames()
        it
    }

    private fun randomNames(): List<String> =
        (0..(0..NAMES_MAX).random()).map { lorem((NAME_WORDS_MIN..NAME_WORDS_MAX).random()).joinToString() }

    private fun lorem(words: Int): Sequence<String> =
        LoremIpsum(words).values
}
