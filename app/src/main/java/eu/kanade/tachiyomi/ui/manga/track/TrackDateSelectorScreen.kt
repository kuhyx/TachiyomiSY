package eu.kanade.tachiyomi.ui.manga.track

import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.presentation.track.TrackDateSelector
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import eu.kanade.tachiyomi.util.lang.toLocalDate
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal data class TrackDateSelectorScreen(
    private val track: Track,
    private val serviceId: Long,
    private val start: Boolean,
) : Screen() {

    @Transient
    private val selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val targetDate = Instant.ofEpochMilli(utcTimeMillis).toLocalDate(ZoneOffset.UTC)

            // Disallow future dates
            if (targetDate > LocalDate.now(ZoneOffset.UTC)) return false

            return when {
                // Disallow setting start date after finish date
                start && track.finishDate > 0 -> {
                    val finishDate = Instant.ofEpochMilli(track.finishDate).toLocalDate(ZoneOffset.UTC)
                    targetDate <= finishDate
                }
                // Disallow setting finish date before start date
                !start && track.startDate > 0 -> {
                    val startDate = Instant.ofEpochMilli(track.startDate).toLocalDate(ZoneOffset.UTC)
                    startDate <= targetDate
                }
                else -> {
                    true
                }
            }
        }

        override fun isSelectableYear(year: Int): Boolean {
            // Disallow future years
            if (year > LocalDate.now(ZoneOffset.UTC).year) return false

            return when {
                // Disallow setting start year after finish year
                start && track.finishDate > 0 -> {
                    val finishDate = Instant.ofEpochMilli(track.finishDate).toLocalDate(ZoneOffset.UTC)
                    year <= finishDate.year
                }
                // Disallow setting finish year before start year
                !start && track.startDate > 0 -> {
                    val startDate = Instant.ofEpochMilli(track.startDate).toLocalDate(ZoneOffset.UTC)
                    startDate.year <= year
                }
                else -> {
                    true
                }
            }
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
                start = start,
            )
        }

        val canRemove = if (start) {
            track.startDate > 0
        } else {
            track.finishDate > 0
        }
        TrackDateSelector(
            title = if (start) {
                stringResource(MR.strings.track_started_reading_date)
            } else {
                stringResource(MR.strings.track_finished_reading_date)
            },
            initialSelectedDateMillis = screenModel.initialSelection,
            selectableDates = selectableDates,
            onConfirm = {
                screenModel.setDate(it)
                navigator.pop()
            },
            onRemove = { screenModel.confirmRemoveDate(navigator) }.takeIf { canRemove },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: Track,
        private val tracker: Tracker,
        private val start: Boolean,
    ) : ScreenModel {

        // In UTC
        val initialSelection: Long
            get() {
                val millis = (if (start) track.startDate else track.finishDate)
                    .takeIf { it != 0L }
                    ?: Instant.now().toEpochMilli()
                return millis.convertEpochMillisZone(ZoneOffset.systemDefault(), ZoneOffset.UTC)
            }

        // In UTC
        fun setDate(millis: Long) {
            // Convert to local time
            val localMillis = millis.convertEpochMillisZone(ZoneOffset.UTC, ZoneOffset.systemDefault())
            screenModelScope.launchNonCancellable {
                if (start) {
                    tracker.setRemoteStartDate(track.toDbTrack(), localMillis)
                } else {
                    tracker.setRemoteFinishDate(track.toDbTrack(), localMillis)
                }
            }
        }

        fun confirmRemoveDate(navigator: Navigator) {
            navigator.push(TrackDateRemoverScreen(track, tracker.id, start))
        }
    }
}

internal fun dateSelector(item: TrackItem, start: Boolean) =
    TrackDateSelectorScreen(track = item.track!!, serviceId = item.tracker.id, start = start)
