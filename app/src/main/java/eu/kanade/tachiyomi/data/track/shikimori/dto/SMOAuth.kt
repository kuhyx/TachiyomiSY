package eu.kanade.tachiyomi.data.track.shikimori.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MILLIS_PER_SECOND = 1000L

// Refresh an hour before the server would reject the token.
private const val EXPIRY_MARGIN_SECONDS = 3600L

@Serializable
internal data class SMOAuth(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("created_at")
    val createdAt: Long,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String?,
)

// Access token lives 1 day
internal fun SMOAuth.isExpired() = System.currentTimeMillis() / MILLIS_PER_SECOND > createdAt + expiresIn - EXPIRY_MARGIN_SECONDS
