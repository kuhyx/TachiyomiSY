package eu.kanade.tachiyomi.data.track.bangumi.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MILLIS_PER_SECOND = 1000L

// Refresh an hour before the server would reject the token.
private const val EXPIRY_MARGIN_SECONDS = 3600L

@Serializable
internal data class BGMOAuth(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("created_at")
    @EncodeDefault
    val createdAt: Long = System.currentTimeMillis() / MILLIS_PER_SECOND,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String?,
    @SerialName("user_id")
    val userId: Long?,
)

// Access token refresh before expired
internal fun BGMOAuth.isExpired() = System.currentTimeMillis() / MILLIS_PER_SECOND > createdAt + expiresIn - EXPIRY_MARGIN_SECONDS
