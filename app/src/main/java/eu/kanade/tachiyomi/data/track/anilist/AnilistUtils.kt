package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.database.models.Track
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

// AniList keeps scores on a 100-point scale; the star and smiley formats bucket it.
private const val POINTS_PER_TEN_POINT_STEP = 10
private const val ONE_STAR_MAX = 30
private const val TWO_STARS_MAX = 50
private const val THREE_STARS_MAX = 70
private const val FOUR_STARS_MAX = 90
private const val SAD_SMILEY_MAX = 35
private const val NEUTRAL_SMILEY_MAX = 60

internal fun Track.toApiStatus() = when (status) {
    Anilist.READING -> "CURRENT"
    Anilist.COMPLETED -> "COMPLETED"
    Anilist.ON_HOLD -> "PAUSED"
    Anilist.DROPPED -> "DROPPED"
    Anilist.PLAN_TO_READ -> "PLANNING"
    Anilist.REREADING -> "REPEATING"
    else -> throw NotImplementedError("Unknown status: $status")
}

private val preferences: TrackPreferences by injectLazy()

internal fun DomainTrack.toApiScore(): String = when (preferences.anilistScoreType.get()) {
    // 10 point
    "POINT_10" -> (score.toInt() / POINTS_PER_TEN_POINT_STEP).toString()
    // 100 point
    "POINT_100" -> score.toInt().toString()
    // 5 stars
    "POINT_5" -> when {
        score == 0.0 -> "0"
        score < ONE_STAR_MAX -> "1"
        score < TWO_STARS_MAX -> "2"
        score < THREE_STARS_MAX -> "3"
        score < FOUR_STARS_MAX -> "4"
        else -> "5"
    }
    // Smiley
    "POINT_3" -> when {
        score == 0.0 -> "0"
        score <= SAD_SMILEY_MAX -> ":("
        score <= NEUTRAL_SMILEY_MAX -> ":|"
        else -> ":)"
    }
    // 10 point decimal
    "POINT_10_DECIMAL" -> (score / POINTS_PER_TEN_POINT_STEP).toString()
    else -> throw NotImplementedError("Unknown score type")
}
