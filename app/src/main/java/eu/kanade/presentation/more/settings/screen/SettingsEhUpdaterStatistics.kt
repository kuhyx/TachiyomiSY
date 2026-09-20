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
    val now = System.currentTimeMillis().milliseconds
    var period: Duration = now - then
    val relativeTime = RelativeTime()
    while (period > 0.milliseconds) {
        when {
            period >= DAYS_PER_YEAR.days -> {
                (period.inWholeDays / DAYS_PER_YEAR).let {
                    relativeTime.years = it
                    period -= (it * DAYS_PER_YEAR).days
                }
                continue
            }
            period >= DAYS_PER_MONTH.days -> {
                (period.inWholeDays / DAYS_PER_MONTH).let {
                    relativeTime.months = it
                    period -= (it * DAYS_PER_MONTH).days
                }
            }
            period >= DAYS_PER_WEEK.days -> {
                (period.inWholeDays / DAYS_PER_WEEK).let {
                    relativeTime.weeks = it
                    period -= (it * DAYS_PER_WEEK).days
                }
            }
            period >= 1.days -> {
                period.inWholeDays.let {
                    relativeTime.days = it
                    period -= it.days
                }
            }
            period >= 1.hours -> {
                period.inWholeHours.let {
                    relativeTime.hours = it
                    period -= it.hours
                }
            }
            period >= 1.minutes -> {
                period.inWholeMinutes.let {
                    relativeTime.minutes = it
                    period -= it.minutes
                }
            }
            period >= 1.seconds -> {
                period.inWholeSeconds.let {
                    relativeTime.seconds = it
                    period -= it.seconds
                }
            }
            period >= 1.milliseconds -> {
                period.inWholeMilliseconds.let {
                    relativeTime.milliseconds = it
                }
                period = Duration.ZERO
            }
        }
    }
    return relativeTime
}

private fun getRelativeTimeString(relativeTime: RelativeTime, context: Context): String {
    return relativeTime.years?.let { context.pluralStringResource(SYMR.plurals.humanize_year, it.toInt(), it) }
        ?: relativeTime.months?.let {
            context.pluralStringResource(SYMR.plurals.humanize_month, it.toInt(), it)
        }
        ?: relativeTime.weeks?.let { context.pluralStringResource(SYMR.plurals.humanize_week, it.toInt(), it) }
        ?: relativeTime.days?.let { context.pluralStringResource(SYMR.plurals.humanize_day, it.toInt(), it) }
        ?: relativeTime.hours?.let { context.pluralStringResource(SYMR.plurals.humanize_hour, it.toInt(), it) }
        ?: relativeTime.minutes?.let {
            context.pluralStringResource(SYMR.plurals.humanize_minute, it.toInt(), it)
        }
        ?: relativeTime.seconds?.let {
            context.pluralStringResource(SYMR.plurals.humanize_second, it.toInt(), it)
        }
        ?: context.stringResource(SYMR.strings.humanize_fallback)
}

internal data class RelativeTime(
    var years: Long? = null,
    var months: Long? = null,
    var weeks: Long? = null,
    var days: Long? = null,
    var hours: Long? = null,
    var minutes: Long? = null,
    var seconds: Long? = null,
    var milliseconds: Long? = null,
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
        if (updateInfo == null) {
            UpdaterStatisticsLoadingDialog()
        } else {
            UpdaterStatisticsDialog(
                onDismissRequest = { dialogOpen = false },
                updateInfo = updateInfo.orEmpty(),
            )
        }
    }
    return Preference.PreferenceItem.TextPreference(
        title = stringResource(SYMR.strings.show_updater_statistics),
        onClick = {
            dialogOpen = true
        },
    )
}
