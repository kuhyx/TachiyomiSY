package eu.kanade.tachiyomi.data.track.hikka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MILLIS_PER_SECOND = 1000L

// Refresh five minutes before the server would reject the token.
private const val EXPIRY_MARGIN_SECONDS = 5L * 60L

@Serializable
internal data class HKOAuth(
    @SerialName("secret")
    val accessToken: String,
    val expiration: Long,
    val created: Long,
)

internal fun HKOAuth.isExpired(): Boolean {
    val currentTime = System.currentTimeMillis() / MILLIS_PER_SECOND
    val buffer = EXPIRY_MARGIN_SECONDS
    return currentTime >= expiration - buffer
}
