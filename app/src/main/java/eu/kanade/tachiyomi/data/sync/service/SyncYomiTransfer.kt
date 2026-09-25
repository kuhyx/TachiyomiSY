package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.service.SyncYomiSyncService.SyncYomiException
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.PUT
import eu.kanade.tachiyomi.network.await
import kotlinx.serialization.SerializationException
import logcat.LogPriority
import logcat.logcat
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.http.HttpStatus
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

private const val UPLOAD_TIMEOUT_SECONDS = 30L

internal suspend fun SyncYomiSyncService.pullSyncData(): Pair<SyncData?, String> {
    val host = syncPreferences.clientHost.get()
    val apiKey = syncPreferences.clientAPIKey.get()
    val downloadUrl = "$host/api/sync/content"

    val headersBuilder = Headers.Builder().add(X_API_TOKEN, apiKey)
    val lastETag = syncPreferences.lastSyncEtag.get()
    if (lastETag != "") {
        headersBuilder.add("If-None-Match", lastETag)
    }
    val headers = headersBuilder.build()

    val downloadRequest = GET(
        url = downloadUrl,
        headers = headers,
    )

    val client = OkHttpClient()
    val response = client.newCall(downloadRequest).await()

    return when {
        // Only an If-None-Match request gets a 304, so lastETag is the one that was sent.
        response.code == HttpStatus.SC_NOT_MODIFIED -> {
            logcat(LogPriority.INFO) {
                "Remote server not modified"
            }
            Pair(null, lastETag)
        }
        // maybe got deleted from remote
        response.code == HttpStatus.SC_NOT_FOUND -> {
            Pair(null, "")
        }
        response.isSuccessful -> {
            decodeSyncData(response)
        }
        else -> {
            val responseBody = response.body.string()
            notifier.showSyncError("Failed to download sync data: $responseBody")
            logcat(LogPriority.ERROR) { "SyncError: $responseBody" }
            throw SyncYomiException("Failed to download sync data: $responseBody")
        }
    }
}

// An undecodable body counts as no remote data, so the next push overwrites it.
internal fun SyncYomiSyncService.decodeSyncData(response: Response): Pair<SyncData?, String> {
    val newETag = response.headers["ETag"]
        .takeIf { it?.isNotEmpty() == true }
        ?: throw SyncYomiException("Missing ETag")

    val byteArray = response.body.byteStream().use { it.readBytes() }

    return try {
        val backup = protoBuf.decodeFromByteArray(Backup.serializer(), byteArray)
        Pair(SyncData(backup = backup), newETag)
    } catch (_: SerializationException) {
        logcat(LogPriority.INFO) {
            "Bad content responsed from server"
        }
        Pair(null, "")
    }
}

// Return true if update success.
internal suspend fun SyncYomiSyncService.pushSyncData(syncData: SyncData, eTag: String): Boolean {
    val backup = syncData.backup ?: return true

    val host = syncPreferences.clientHost.get()
    val apiKey = syncPreferences.clientAPIKey.get()
    val uploadUrl = "$host/api/sync/content"
    val timeout = UPLOAD_TIMEOUT_SECONDS

    val headersBuilder = Headers.Builder().add(X_API_TOKEN, apiKey)
    if (eTag.isNotEmpty()) {
        headersBuilder.add("If-Match", eTag)
    }
    val headers = headersBuilder.build()

    // Set timeout to 30 seconds
    val client = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS)
        .writeTimeout(timeout, TimeUnit.SECONDS)
        .build()

    val byteArray = protoBuf.encodeToByteArray(Backup.serializer(), backup)
    if (byteArray.isEmpty()) {
        throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
    }
    val body = byteArray.toRequestBody("application/octet-stream".toMediaType())

    val uploadRequest = PUT(
        url = uploadUrl,
        headers = headers,
        body = body,
    )

    val response = client.newCall(uploadRequest).await()

    return when {
        response.isSuccessful -> {
            val newETag = response.headers["ETag"]
                .takeIf { it?.isNotEmpty() == true }
                ?: throw SyncYomiException("Missing ETag")
            syncPreferences.lastSyncEtag.set(newETag)
            logcat(LogPriority.DEBUG) { "SyncYomi sync completed" }
            true
        }
        // other clients updated remote data, will try next time
        response.code == HttpStatus.SC_PRECONDITION_FAILED -> {
            logcat(LogPriority.DEBUG) { "SyncYomi sync failed with 412" }
            false
        }
        else -> {
            val responseBody = response.body.string()
            notifier.showSyncError("Failed to upload sync data: $responseBody")
            logcat(LogPriority.ERROR) { "SyncError: $responseBody" }
            false
        }
    }
}
