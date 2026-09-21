package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val APPLICATION_OCTET_STREAM = "application/octet-stream"
private const val DEVICE_ID = "deviceId"

internal class GoogleDriveSyncService(context: Context, json: Json, syncPreferences: SyncPreferences) : SyncService(
    context,
    json,
    syncPreferences,
) {
    private val appName = context.stringResource(MR.strings.app_name)

    private val remoteFileName = "${appName}_sync.proto.gz"

    private val googleDriveService = GoogleDriveService(context)

    private val protoBuf: ProtoBuf = Injekt.get()

    constructor(context: Context) : this(
        context,
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        },
        Injekt.get<SyncPreferences>(),
    )

    enum class DeleteSyncDataStatus {
        NOT_INITIALIZED,
        NO_FILES,
        SUCCESS,
        ERROR,
    }

    override suspend fun doSync(syncData: SyncData): Backup? {
        beforeSync()

        try {
            val remoteSData = pullSyncData()
            val toPush = if (remoteSData == null) syncData else reconcile(syncData, remoteSData)
            pushSyncData(toPush)
            return toPush.backup
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, "SyncService") { "Error syncing: ${expected.message}" }
            return null
        }
    }

    private suspend fun beforeSync() {
        googleDriveService.refreshToken()
    }

    // The last device to sync overwrites the remote copy; any other device merges into it.
    private fun reconcile(syncData: SyncData, remoteSData: SyncData): SyncData {
        val localDeviceId = syncPreferences.uniqueDeviceID()
        val lastSyncDeviceId = remoteSData.deviceId
        logcat(LogPriority.DEBUG, "SyncService") {
            "Local device ID: $localDeviceId, Last sync device ID: $lastSyncDeviceId"
        }
        return if (lastSyncDeviceId == localDeviceId) syncData else mergeSyncData(syncData, remoteSData)
    }

    private fun pullSyncData(): SyncData? {
        val drive = googleDriveService.driveService
            ?: error(context.stringResource(SYMR.strings.google_drive_not_signed_in))

        val fileList = getAppDataFileList(drive)
        if (fileList.isEmpty()) {
            logcat(LogPriority.INFO) { "No files found in app data" }
            return null
        }

        val gdriveFileId = fileList[0].id
        logcat(LogPriority.DEBUG) { "Google Drive File ID: $gdriveFileId" }

        try {
            drive.files().get(gdriveFileId).executeMediaAsInputStream().use { inputStream ->
                GZIPInputStream(inputStream).use { gzipInputStream ->
                    val byteArray = gzipInputStream.readBytes()
                    val backup = protoBuf.decodeFromByteArray(Backup.serializer(), byteArray)
                    val deviceId = fileList[0].appProperties[DEVICE_ID] ?: ""
                    return SyncData(deviceId = deviceId, backup = backup)
                }
            }
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, throwable = expected) { "Error downloading file" }
            throw IOException("Failed to download sync data: ${expected.message}", expected)
        }
    }

    private suspend fun pushSyncData(syncData: SyncData) {
        val drive = googleDriveService.driveService
            ?: error(context.stringResource(SYMR.strings.google_drive_not_signed_in))

        val fileList = getAppDataFileList(drive)
        val backup = syncData.backup ?: return

        val byteArray = protoBuf.encodeToByteArray(Backup.serializer(), backup)
        if (byteArray.isEmpty()) {
            throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
        }

        PipedOutputStream().use { pos ->
            PipedInputStream(pos).use { pis ->
                withIOContext {
                    launch {
                        GZIPOutputStream(pos).use { gzipOutputStream ->
                            gzipOutputStream.write(byteArray)
                        }
                    }

                    val mediaContent = InputStreamContent(APPLICATION_OCTET_STREAM, pis)

                    if (fileList.isNotEmpty()) {
                        val fileId = fileList[0].id
                        val fileMetadata = File().apply {
                            name = remoteFileName
                            mimeType = APPLICATION_OCTET_STREAM
                            appProperties = mapOf(DEVICE_ID to syncData.deviceId)
                        }
                        drive.files().update(fileId, fileMetadata, mediaContent).execute()
                        logcat(LogPriority.DEBUG) {
                            "Updated existing sync data file in Google Drive with file ID: $fileId"
                        }
                    } else {
                        val fileMetadata = File().apply {
                            name = remoteFileName
                            mimeType = APPLICATION_OCTET_STREAM
                            parents = listOf("appDataFolder")
                            appProperties = mapOf(DEVICE_ID to syncData.deviceId)
                        }
                        val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute()
                        logcat(LogPriority.DEBUG) {
                            "Created new sync data file in Google Drive with file ID: ${uploadedFile.id}"
                        }
                    }
                }
            }
        }
    }

    private fun getAppDataFileList(drive: Drive): MutableList<File> {
        try {
            // Search for the existing file by name in the appData folder
            val query = "mimeType='application/x-gzip' and name = '$remoteFileName'"
            val fileList = drive.files()
                .list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .setFields("files(id, name, createdTime, appProperties)")
                .execute()
                .files
            logcat { "AppData folder file list: $fileList" }

            return fileList
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, throwable = expected) { "Error no sync data found in appData folder" }
            return mutableListOf()
        }
    }

    suspend fun deleteSyncDataFromGoogleDrive(): DeleteSyncDataStatus {
        val drive = googleDriveService.driveService

        if (drive == null) {
            logcat(LogPriority.ERROR) { "Google Drive service not initialized" }
            return DeleteSyncDataStatus.NOT_INITIALIZED
        }
        googleDriveService.refreshToken()

        return withIOContext {
            try {
                val appDataFileList = getAppDataFileList(drive)

                if (appDataFileList.isEmpty()) {
                    this@GoogleDriveSyncService
                        .logcat(LogPriority.DEBUG) { "No sync data file found in appData folder of Google Drive" }
                    DeleteSyncDataStatus.NO_FILES
                } else {
                    for (file in appDataFileList) {
                        drive.files().delete(file.id).execute()
                        this@GoogleDriveSyncService.logcat(
                            LogPriority.DEBUG,
                        ) { "Deleted sync data file in appData folder of Google Drive with file ID: ${file.id}" }
                    }
                    DeleteSyncDataStatus.SUCCESS
                }
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                this@GoogleDriveSyncService.logcat(LogPriority.ERROR, throwable = expected) {
                    "Error occurred while interacting with Google Drive"
                }
                DeleteSyncDataStatus.ERROR
            }
        }
    }
}
