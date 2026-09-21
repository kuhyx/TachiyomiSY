package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.SyncNotifier
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.logcat
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal const val X_API_TOKEN = "X-API-Token"

internal class SyncYomiSyncService(
    context: Context,
    json: Json,
    syncPreferences: SyncPreferences,
    internal val notifier: SyncNotifier,

    internal val protoBuf: ProtoBuf = Injekt.get(),
) : SyncService(context, json, syncPreferences) {

    internal class SyncYomiException(message: String?) : Exception(message)

    @Serializable
    private data class SyncEvent(
        val event: SyncEventStatus,
        @SerialName("device_name")
        val deviceName: String? = null,
        val message: String? = null,
    )

    @Serializable
    private enum class SyncEventStatus {
        SYNC_STARTED,
        SYNC_SUCCESS,
        SYNC_FAILED,
        SYNC_ERROR,
        SYNC_CANCELLED,
    }

    override suspend fun doSync(syncData: SyncData): Backup? {
        reportSyncEvent(SyncEventStatus.SYNC_STARTED)

        try {
            val (remoteData, etag) = pullSyncData()

            val finalSyncData = if (remoteData != null) {
                assert(etag.isNotEmpty()) { "ETag should never be empty if remote data is not null" }
                logcat(LogPriority.DEBUG, "SyncService") {
                    "Try update remote data with ETag($etag)"
                }
                mergeSyncData(syncData, remoteData)
            } else {
                // init or overwrite remote data
                logcat(LogPriority.DEBUG) {
                    "Try overwrite remote data with ETag($etag)"
                }
                syncData
            }

            // The merge guard only runs when there is remote data to merge against. When
            // the server replies 304 the local payload is pushed verbatim, so it has to
            // be checked against what this device pushed last time as well -- that is the
            // path a collapsed library takes to overwrite a healthy server copy.
            val entryCount = finalSyncData.backup?.backupManga?.size ?: 0
            assertNoLibraryCollapse(entryCount)

            val success = pushSyncData(finalSyncData, etag)

            if (success) {
                // Never let a zero-entry push become the baseline: assertNoLibraryCollapse
                // treats a baseline of 0 as "no baseline yet" and would stay disabled from
                // then on. pushSyncData also short-circuits to true on a null backup.
                if (entryCount > 0) {
                    syncPreferences.lastSyncEntryCount.set(entryCount)
                }
                reportSyncEvent(SyncEventStatus.SYNC_SUCCESS)
            } else {
                reportSyncEvent(SyncEventStatus.SYNC_FAILED, "Failed to push sync data")
            }

            return finalSyncData.backup
        } catch (cancelled: CancellationException) {
            reportSyncEvent(SyncEventStatus.SYNC_CANCELLED, cancelled.message)
            throw cancelled
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR) { "Error syncing: ${expected.message}" }
            notifier.showSyncError(expected.message)
            reportSyncEvent(SyncEventStatus.SYNC_ERROR, expected.message)
            return null
        }
    }

    private suspend fun reportSyncEvent(event: SyncEventStatus, message: String? = null) {
        withContext(NonCancellable) {
            try {
                val host = syncPreferences.clientHost.get()
                val apiKey = syncPreferences.clientAPIKey.get()
                val url = "$host/api/sync/event"

                val headersBuilder = Headers.Builder().add(X_API_TOKEN, apiKey)
                val headers = headersBuilder.build()

                val bodyObj = SyncEvent(
                    event = event,
                    deviceName = android.os.Build.MODEL,
                    message = message,
                )

                val jsonBody = json.encodeToString(SyncEvent.serializer(), bodyObj)
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = POST(
                    url = url,
                    headers = headers,
                    body = requestBody,
                )

                val client = OkHttpClient()
                client.newCall(request).await().close()
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.ERROR) { "Failed to report sync event: ${expected.message}" }
            }
        }
    }
}
