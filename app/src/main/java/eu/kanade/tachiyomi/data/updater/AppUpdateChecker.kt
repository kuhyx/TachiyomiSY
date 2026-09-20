package eu.kanade.tachiyomi.data.updater

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import exh.SY_DEBUG_VERSION
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.release.interactor.GetApplicationRelease
import uy.kohesive.injekt.injectLazy

internal class AppUpdateChecker {

    private val getApplicationRelease: GetApplicationRelease by injectLazy()

    suspend fun checkForUpdate(context: Context, forceCheck: Boolean = false): GetApplicationRelease.Result {
        // Disable app update checks for older Android versions that we're going to drop support for
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        //     return GetApplicationRelease.Result.OsTooOld
        // }

        return withIOContext {
            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    // SY -->
                    isPreviewBuildType,
                    // SY <--
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    // SY -->
                    SY_DEBUG_VERSION,
                    // SY <--
                    forceCheck,
                ),
            )

            if (result is GetApplicationRelease.Result.NewUpdate) {
                AppUpdateNotifier(context).promptUpdate(result.release)
            }

            result
        }
    }
}

internal val GITHUB_REPO: String by lazy {
    // SY -->
    if (isPreviewBuildType) {
        "jobobby04/TachiyomiSYPreview"
    } else {
        "jobobby04/tachiyomiSY"
    }
    // SY <--
}
