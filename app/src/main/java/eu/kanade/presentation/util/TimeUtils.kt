package eu.kanade.presentation.util

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal fun Duration.toDurationString(context: Context, fallback: String): String {
    return toComponents { days, hours, minutes, seconds, _ ->
        // Never more than two parts: minutes drop once both days and hours show, seconds once either does.
        val showMinutes = minutes != 0 && (days == 0L || hours == 0)
        val showSeconds = seconds != 0 && days == 0L && hours == 0
        listOfNotNull(
            context.stringResource(MR.strings.day_short, days).takeIf { days != 0L },
            context.stringResource(MR.strings.hour_short, hours).takeIf { hours != 0 },
            context.stringResource(MR.strings.minute_short, minutes).takeIf { showMinutes },
            context.stringResource(MR.strings.seconds_short, seconds).takeIf { showSeconds },
        ).joinToString(" ").ifBlank { fallback }
    }
}

@Composable
@ReadOnlyComposable
internal fun relativeTimeSpanString(epochMillis: Long): String {
    val now = Instant.now().toEpochMilli()
    return when {
        epochMillis <= 0L -> stringResource(MR.strings.relative_time_span_never)
        now - epochMillis < 1.minutes.inWholeMilliseconds -> stringResource(
            MR.strings.updates_last_update_info_just_now,
        )
        else -> DateUtils.getRelativeTimeSpanString(epochMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
}
