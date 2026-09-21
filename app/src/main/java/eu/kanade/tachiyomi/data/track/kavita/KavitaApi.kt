package eu.kanade.tachiyomi.data.track.kavita

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import logcat.LogPriority
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException

internal class KavitaApi(private val client: OkHttpClient, interceptor: KavitaInterceptor) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder()
        .dns(Dns.SYSTEM)
        .addInterceptor(interceptor)
        .build()

    fun getApiFromUrl(url: String): String = url.split("/api/").first() + "/api"

    /*
     * Uses url to compare against each source APIURL's to get the correct custom source preference.
     * Now having source preference we can do getString("APIKEY")
     * Authenticates to get the token
     * Saves the token in the var jwtToken
     */
    fun getNewToken(apiUrl: String, apiKey: String): String? {
        val request = POST(
            "$apiUrl/Plugin/authenticate?apiKey=$apiKey&pluginName=Tachiyomi-Kavita",
            body = "{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
        )
        try {
            client.newCall(request).execute().use { return readToken(it, apiUrl, apiKey) }
            // Not sure which one to catch
        } catch (_: SocketTimeoutException) {
            logcat(LogPriority.WARN) {
                "Could not fetch JWT token. Probably due to connectivity issue or URL '$apiUrl' not available, skipping"
            }
            return null
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR) {
                "Unhandled exception fetching JWT token for URL: '$apiUrl'"
            }
            throw IOException(expected)
        }
    }

    // The token on 200; 401 and 500 are the server saying the key is wrong or it is broken.
    private fun readToken(response: Response, apiUrl: String, apiKey: String): String? = when (response.code) {
        HttpURLConnection.HTTP_OK -> {
            with(json) { response.parseAs<AuthenticationDto>().token }
        }
        HttpURLConnection.HTTP_UNAUTHORIZED -> {
            logcat(LogPriority.WARN) {
                "Unauthorized / API key not valid: API URL: $apiUrl, empty API key: ${apiKey.isEmpty()}"
            }
            throw IOException("Unauthorized / api key not valid")
        }
        HttpURLConnection.HTTP_INTERNAL_ERROR -> {
            logcat(LogPriority.WARN) {
                "Error fetching JWT token. API URL: $apiUrl, empty API key: ${apiKey.isEmpty()}"
            }
            throw IOException("Error fetching JWT token")
        }
        else -> {
            null
        }
    }

    private fun getApiVolumesUrl(url: String): String =
        "${getApiFromUrl(url)}/Series/volumes?seriesId=${getIdFromUrl(url)}"

    /* Strips serie id from URL */
    private fun getIdFromUrl(url: String): Int = url.substringAfterLast("/").toInt()

    /*
     * Returns total chapters in the series.
     * Ignores volumes.
     * Volumes consisting of 1 file treated as chapter
     */
    private fun getTotalChapters(url: String): Long {
        val requestUrl = getApiVolumesUrl(url)
        try {
            val listVolumeDto = with(json) {
                authClient.newCall(GET(requestUrl))
                    .execute()
                    .parseAs<List<VolumeDto>>()
            }
            var volumeNumber = 0L
            var maxChapterNumber = 0L
            for (volume in listVolumeDto) {
                val lastChapter = volume.chapters.maxOf { it.number!!.toFloat() }
                if (lastChapter == 0f) {
                    volumeNumber++
                } else if (maxChapterNumber < lastChapter) {
                    maxChapterNumber = lastChapter.toLong()
                }
            }

            return if (maxChapterNumber > volumeNumber) maxChapterNumber else volumeNumber
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.WARN, expected) { "Exception fetching Total Chapters. Request:$requestUrl" }
            throw expected
        }
    }

    private fun getLatestChapterRead(url: String): Double {
        val seriesId = getIdFromUrl(url)
        val requestUrl = "${getApiFromUrl(url)}/Tachiyomi/latest-chapter?seriesId=$seriesId"
        return try {
            authClient.newCall(GET(requestUrl)).execute().use { it.latestChapterNumber() }
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(
                LogPriority.WARN,
                expected,
            ) { "Exception getting latest chapter read. Could not get itemRequest: $requestUrl" }
            throw expected
        }
    }

    // 200 carries the chapter; 204 means nothing read yet, and anything else counts the same.
    private fun Response.latestChapterNumber(): Double = if (code == HttpURLConnection.HTTP_OK) {
        with(json) { parseAs<ChapterDto>().number!!.replace(",", ".").toDouble() }
    } else {
        0.0
    }

    suspend fun getTrackSearch(url: String): TrackSearch = withIOContext {
        try {
            val seriesDto: SeriesDto = with(json) {
                authClient.newCall(GET(url))
                    .awaitSuccess()
                    .parseAs()
            }

            val track = seriesDto.toTrack()
            track.apply {
                coverUrl = seriesDto.thumbnailUrl.toString()
                trackingUrl = url
                totalChapters = getTotalChapters(url)

                title = seriesDto.name
                status = when (seriesDto.pagesRead) {
                    seriesDto.pages -> Kavita.COMPLETED
                    0 -> Kavita.UNREAD
                    else -> Kavita.READING
                }
                lastChapterRead = getLatestChapterRead(url)
            }
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.WARN, expected) { "Could not get item: $url" }
            throw expected
        }
    }

    suspend fun updateProgress(track: Track): Track {
        val requestUrl = "${getApiFromUrl(
            track.trackingUrl,
        )}/Tachiyomi/mark-chapter-until-as-read?seriesId=${getIdFromUrl(
            track.trackingUrl,
        )}&chapterNumber=${track.lastChapterRead}"
        authClient.newCall(
            POST(requestUrl, body = "{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())),
        )
            .awaitSuccess()
        return getTrackSearch(track.trackingUrl)
    }
}
