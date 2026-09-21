@file:OptIn(ExperimentalAtomicApi::class)

package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.workDataOf
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob.Target
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val FLEX_MINUTES = 10L
private const val BACKOFF_MINUTES = 10L

internal fun LibraryUpdateJob.Companion.setupTask(
    context: Context,
    prefInterval: Int? = null,
) {
    val preferences = Injekt.get<LibraryPreferences>()
    val interval = prefInterval ?: preferences.autoUpdateInterval.get()
    if (interval > 0) {
        val restrictions = preferences.autoUpdateDeviceRestrictions.get()
        val networkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }
        val networkRequest = NetworkRequest.Builder().apply {
            removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            if (DEVICE_ONLY_ON_WIFI in restrictions) {
                addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            }
            if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }
        }
            .build()
        val constraints = Constraints.Builder()
            // 'networkRequest' only applies to Android 9+, otherwise 'networkType' is used
            .setRequiredNetworkRequest(networkRequest, networkType)
            .setRequiresCharging(DEVICE_CHARGING in restrictions)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
            interval.toLong(),
            TimeUnit.HOURS,
            FLEX_MINUTES,
            TimeUnit.MINUTES,
        )
            .addTag(TAG)
            .addTag(WORK_NAME_AUTO)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()

        context.workManager.enqueueUniquePeriodicWork(
            WORK_NAME_AUTO,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    } else {
        context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
    }
}

internal fun LibraryUpdateJob.Companion.startNow(
    context: Context,
    category: Category? = null,
    target: Target = Target.CHAPTERS,
    // SY -->
    group: Int = LibraryGroup.BY_DEFAULT,
    groupExtra: String? = null,
    // SY <--
): Boolean {
    // Already running either as a scheduled or manual job
    if (context.workManager.isRunning(TAG)) return false

    // Always sync the data before library update if syncing is enabled; a sync already running wins.
    val syncFirst = Injekt.get<SyncPreferences>().isSyncEnabled()
    if (syncFirst && SyncDataJob.isRunning(context)) return false

    val inputData = workDataOf(
        KEY_CATEGORY to category?.id,
        KEY_TARGET to target.name,
        // SY -->
        KEY_GROUP to group,
        KEY_GROUP_EXTRA to groupExtra,
        // SY <--
    )
    val libraryUpdateJob = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
        .addTag(TAG)
        .addTag(WORK_NAME_MANUAL)
        .setInputData(inputData)
        .build()

    val wm = context.workManager
    if (syncFirst) {
        // Chain SyncDataJob to run before LibraryUpdateJob
        val syncDataJob = OneTimeWorkRequestBuilder<SyncDataJob>()
            .addTag(SyncDataJob.TAG_MANUAL)
            .build()
        wm.beginUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, syncDataJob)
            .then(libraryUpdateJob)
            .enqueue()
    } else {
        wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, libraryUpdateJob)
    }

    return true
}

internal fun LibraryUpdateJob.Companion.stop(context: Context) {
    val wm = context.workManager
    val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
        .addStates(listOf(WorkInfo.State.RUNNING))
        .build()
    wm.getWorkInfos(workQuery).get()
        // Should only return one work but just in case
        .forEach {
            wm.cancelWorkById(it.id)

            // Re-enqueue cancelled scheduled work
            if (it.tags.contains(WORK_NAME_AUTO)) {
                setupTask(context)
            }
        }
}
