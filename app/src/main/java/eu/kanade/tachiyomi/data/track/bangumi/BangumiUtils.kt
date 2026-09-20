package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.data.database.models.Track

// Bangumi's collection types are numbered exactly like the tracker's status constants.
internal fun Track.toApiStatus(): Int = status.takeIf { it in Bangumi.PLAN_TO_READ..Bangumi.DROPPED }?.toInt()
    ?: throw IllegalArgumentException("Unknown status: $status")
