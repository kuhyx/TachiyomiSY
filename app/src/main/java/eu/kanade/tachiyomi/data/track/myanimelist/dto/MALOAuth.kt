package eu.kanade.tachiyomi.data.track.myanimelist.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MILLIS_PER_SECOND = 1000L

// Refresh a minute before the server would reject the token.
private const val EXPIRY_MARGIN_SECONDS = 60L

@Serializable
internal data class MALOAuth(
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("created_at")
    @EncodeDefault
    val createdAt: Long = System.currentTimeMillis() / MILLIS_PER_SECOND,
)

// Assumes expired a minute earlier
internal fun MALOAuth.isExpired() =
    createdAt + expiresIn - EXPIRY_MARGIN_SECONDS < System.currentTimeMillis() / MILLIS_PER_SECOND
