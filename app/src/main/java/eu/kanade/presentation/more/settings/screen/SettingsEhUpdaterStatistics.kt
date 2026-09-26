package eu.kanade.presentation.more.settings.screen

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.more.settings.Preference
import exh.eh.EHentaiUpdaterStats
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.base.raise
import exh.source.ExhPreferences
import exh.util.nullIfBlank
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetExhFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val DAYS_PER_YEAR = 365
private const val DAYS_PER_MONTH = 30
private const val DAYS_PER_WEEK = 7

/*
 * The gallery-updater statistics preference and its dialogs.
 * Part of [SettingsEhScreen]; same package, so its getPreferences() calls them as before.
 */

@Composable
internal fun UpdaterStatisticsLoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.sizeIn(minWidth = 280.dp, maxWidth = 560.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(SYMR.strings.gallery_updater_statistics_collection),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(40.dp),
            )
        }
    }
}

@Composable
internal fun UpdaterStatisticsDialog(
    onDismissRequest: () -> Unit,
    updateInfo: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(SYMR.strings.gallery_updater_statistics))
        },
        text = {
            Text(text = updateInfo)
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

private fun getRelativeTimeFromNow(then: Duration): RelativeTime {
    val remainder = PeriodRemainder(System.currentTimeMillis().milliseconds - then)
    return RelativeTime(
        years = remainder.take(DAYS_PER_YEAR.days),
        months = remainder.take(DAYS_PER_MONTH.days),
        weeks = remainder.take(DAYS_PER_WEEK.days),
        days = remainder.take(1.days),
        hours = remainder.take(1.hours),
        minutes = remainder.take(1.minutes),
        seconds = remainder.take(1.seconds),
        milliseconds = remainder.take(1.milliseconds),
    )
}

/** What is left of a period as its units are peeled off, largest first. */
private class PeriodRemainder(private var period: Duration) {
    /** How many whole [unit]s fit, taken off the remainder; null when none do. */
    fun take(unit: Duration): Long? {
        val count = (period / unit).toLong().takeIf { it > 0 } ?: return null
        period -= unit * count.toInt()
        return count
    }
}

private fun getRelativeTimeString(relativeTime: RelativeTime, context: Context): String {
    // Largest unit first; milliseconds never get a label.
    val units = listOf(
        relativeTime.years to SYMR.plurals.humanize_year,
        relativeTime.months to SYMR.plurals.humanize_month,
        relativeTime.weeks to SYMR.plurals.humanize_week,
        relativeTime.days to SYMR.plurals.humanize_day,
        relativeTime.hours to SYMR.plurals.humanize_hour,
        relativeTime.minutes to SYMR.plurals.humanize_minute,
        relativeTime.seconds to SYMR.plurals.humanize_second,
    )
    val largest = units.firstNotNullOfOrNull { (count, plural) -> count?.let { it to plural } }
    return largest?.let { (count, plural) -> context.pluralStringResource(plural, count.toInt(), count) }
        ?: context.stringResource(SYMR.strings.humanize_fallback)
}

internal data class RelativeTime(
    val years: Long? = null,
    val months: Long? = null,
    val weeks: Long? = null,
    val days: Long? = null,
    val hours: Long? = null,
    val minutes: Long? = null,
    val seconds: Long? = null,
    val milliseconds: Long? = null,
)

@Composable
internal fun updaterStatistics(
    exhPreferences: ExhPreferences,
    getExhFavoriteMangaWithMetadata: GetExhFavoriteMangaWithMetadata,
    getFlatMetadataById: GetFlatMetadataById,
): Preference.PreferenceItem.TextPreference {
    val context = LocalContext.current
    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen) {
        val updateInfo by produceState<String?>(null) {
            value = withIOContext {
                try {
                    val stats =
                        exhPreferences.exhAutoUpdateStats.get().nullIfBlank()?.let {
                            Json.decodeFromString<EHentaiUpdaterStats>(it)
                        }

                    val statsText = if (stats != null) {
                        context.stringResource(
                            SYMR.strings.gallery_updater_stats_text,
                            getRelativeTimeString(getRelativeTimeFromNow(stats.startTime.milliseconds), context),
                            stats.updateCount,
                            stats.possibleUpdates,
                        )
                    } else {
                        context.stringResource(SYMR.strings.gallery_updater_not_ran_yet)
                    }

                    val allMeta = getExhFavoriteMangaWithMetadata.await()
                        .mapNotNull {
                            getFlatMetadataById.await(it.id)
                                ?.raise(EHentaiSearchMetadata::class)
                        }

                    fun metaInRelativeDuration(duration: Duration): Int {
                        val durationMs = duration.inWholeMilliseconds
                        return allMeta.asSequence().filter {
                            System.currentTimeMillis() - it.lastUpdateCheck < durationMs
                        }.count()
                    }

                    statsText + "\n\n" + context.stringResource(
                        SYMR.strings.gallery_updater_stats_time,
                        metaInRelativeDuration(1.hours),
                        metaInRelativeDuration(6.hours),
                        metaInRelativeDuration(12.hours),
                        metaInRelativeDuration(1.days),
                        metaInRelativeDuration(2.days),
                        metaInRelativeDuration(7.days),
                        metaInRelativeDuration(30.days),
                        metaInRelativeDuration(365.days),
                    )
                } catch (expected: Exception) {
                    // Logged whatever the cause; the caller carries on.
                    logcat(LogPriority.ERROR, expected) { "Error loading gallery update info" }
                    ""
                }
            }
        }
        updateInfo?.let { info ->
            UpdaterStatisticsDialog(
                onDismissRequest = { dialogOpen = false },
                updateInfo = info,
            )
        } ?: UpdaterStatisticsLoadingDialog()
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.show_updater_statistics),
        onClick = {
            dialogOpen = true
        },
    )
}
