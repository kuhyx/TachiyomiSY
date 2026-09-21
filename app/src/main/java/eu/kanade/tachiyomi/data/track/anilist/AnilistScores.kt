package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.anilist.Anilist.Companion.POINT_10
import eu.kanade.tachiyomi.data.track.anilist.Anilist.Companion.POINT_100
import eu.kanade.tachiyomi.data.track.anilist.Anilist.Companion.POINT_10_DECIMAL
import eu.kanade.tachiyomi.data.track.anilist.Anilist.Companion.POINT_3
import eu.kanade.tachiyomi.data.track.anilist.Anilist.Companion.POINT_5
import tachiyomi.domain.track.model.Track as DomainTrack

private const val POINTS_PER_TEN_POINT_STEP = 10.0

private const val TEN_POINT_MAX = 10
private const val HUNDRED_POINT_MAX = 100
private const val FIVE_STAR_MAX = 5
private const val DECIMAL_STEPS_PER_POINT = 10f
private const val POINTS_PER_STAR = 20.0
private const val STAR_OFFSET = 10.0
private const val POINTS_PER_SMILEY = 25.0
private const val SMILEY_OFFSET = 10.0
private const val SAD_SMILEY_MAX = 35
private const val NEUTRAL_SMILEY_MAX = 60

internal fun Anilist.scoreList(): List<String> {
    return when (scorePreference.get()) {
        // 10 point
        POINT_10 -> IntRange(0, TEN_POINT_MAX).map(Int::toString)
        // 100 point
        POINT_100 -> IntRange(0, HUNDRED_POINT_MAX).map(Int::toString)
        // 5 stars
        POINT_5 -> IntRange(0, FIVE_STAR_MAX).map { "$it ★" }
        // Smiley
        POINT_3 -> listOf("-", "😦", "😐", "😊")
        // 10 point decimal
        POINT_10_DECIMAL -> IntRange(0, HUNDRED_POINT_MAX).map { (it / DECIMAL_STEPS_PER_POINT).toString() }
        else -> error("Unknown score type")
    }
}

internal fun Anilist.scoreForIndex(index: Int): Double {
    return when (scorePreference.get()) {
        // 10 point
        POINT_10 -> index * POINTS_PER_TEN_POINT_STEP
        // 100 point
        POINT_100 -> index.toDouble()
        // 5 stars
        POINT_5 -> if (index == 0) {
            0.0
        } else {
            index * POINTS_PER_STAR - STAR_OFFSET
        }
        // Smiley
        POINT_3 -> if (index == 0) {
            0.0
        } else {
            index * POINTS_PER_SMILEY + SMILEY_OFFSET
        }
        // 10 point decimal
        POINT_10_DECIMAL -> index.toDouble()
        else -> error("Unknown score type")
    }
}

internal fun Anilist.displayedScore(track: DomainTrack): String {
    val score = track.score

    return when (scorePreference.get()) {
        POINT_5 -> if (score == 0.0) {
            "0 ★"
        } else {
            "${((score + STAR_OFFSET) / POINTS_PER_STAR).toInt()} ★"
        }

        POINT_3 -> when {
            score == 0.0 -> "0"
            score <= SAD_SMILEY_MAX -> "😦"
            score <= NEUTRAL_SMILEY_MAX -> "😐"
            else -> "😊"
        }

        else -> track.toApiScore()
    }
}
