package exh.eh

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import eu.kanade.tachiyomi.util.system.workManager
import exh.source.ExhPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

private const val FLEX_MINUTES = 10L

internal fun EHentaiUpdateWorker.Companion.launchBackgroundTest(context: Context) {
    context.workManager.enqueue(
        OneTimeWorkRequestBuilder<EHentaiUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG)
            .build(),
    )
}

internal fun EHentaiUpdateWorker.Companion.scheduleBackground(
    context: Context,
    prefInterval: Int? = null,
    prefRestrictions: Set<String>? = null,
) {
    val exhPreferences = Injekt.get<ExhPreferences>()
    val interval = prefInterval ?: exhPreferences.exhAutoUpdateFrequency.get()
    if (interval > 0) {
        val restrictions = prefRestrictions ?: exhPreferences.exhAutoUpdateRequirements.get()
        val acRestriction = DEVICE_CHARGING in restrictions

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(acRestriction)
            .build()

        val request = PeriodicWorkRequestBuilder<EHentaiUpdateWorker>(
            interval.toLong(),
            TimeUnit.HOURS,
            FLEX_MINUTES,
            TimeUnit.MINUTES,
        )
            .addTag(TAG)
            .setConstraints(constraints)
            .build()

        context.workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
        logger.d("Successfully scheduled background update job!")
    } else {
        cancelBackground(context)
    }
}

internal fun EHentaiUpdateWorker.Companion.cancelBackground(context: Context) {
    context.workManager.cancelAllWorkByTag(TAG)
}
