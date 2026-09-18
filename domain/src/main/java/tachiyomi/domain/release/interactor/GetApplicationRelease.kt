package tachiyomi.domain.release.interactor

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService
import java.time.Instant
import java.time.temporal.ChronoUnit

/** The app update check: asks GitHub for the latest release and compares it with the running build. */
public class GetApplicationRelease(
    private val service: ReleaseService,
    private val preferenceStore: PreferenceStore,
) {

    private val lastChecked: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_app_check"), 0)
    }

    /**
     * [Result.NewUpdate] when the latest release of the repository is newer than the running build,
     * else [Result.NoNewUpdate]. Checks at most once every 3 days unless [Arguments.forceCheck] is
     * set, answering [Result.NoNewUpdate] in between; a failed fetch throws.
     */
    public suspend fun await(arguments: Arguments): Result {
        val now = Instant.now()

        val nextCheckTime = Instant.ofEpochMilli(lastChecked.get()).plus(CHECK_INTERVAL_DAYS, ChronoUnit.DAYS)
        if (!arguments.forceCheck && now.isBefore(nextCheckTime)) {
            return Result.NoNewUpdate
        }

        val release = service.latest(arguments.repository)

        lastChecked.set(now.toEpochMilli())

        // Check if latest version is different from current version
        // SY -->
        val isNewVersion =
            isNewVersion(arguments.isPreview, arguments.syDebugVersion, arguments.versionName, release.version)
        // SY <--
        return if (isNewVersion) Result.NewUpdate(release) else Result.NoNewUpdate
    }

    // SY -->
    private fun isNewVersion(
        isPreview: Boolean,
        syDebugVersion: String,
        versionName: String,
        versionTag: String,
    ): Boolean {
        // Removes prefixes like "r" or "v"
        val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")
        return if (isPreview) {
            // Preview builds: based on releases in "jobobby04/TachiyomiSYPreview" repo
            // tagged as something like "508"
            val currentInt = syDebugVersion.toIntOrNull()
            currentInt != null && newVersion.toInt() > currentInt
        } else {
            // Release builds: based on releases in "jobobby04/TachiyomiSY" repo
            // tagged as something like "0.1.2"
            val oldVersion = versionName.replace("[^\\d.]".toRegex(), "")

            val newSemVer = newVersion.split(".").map { it.toInt() }
            val oldSemVer = oldVersion.split(".").map { it.toInt() }

            oldSemVer.mapIndexed { index, i ->
                if (newSemVer[index] > i) {
                    return true
                }
            }

            false
        }
    }
    // SY <--

    /**
     * What the check needs to know about the running build.
     *
     * @property isPreview Whether this is a preview build, compared by build number instead of version.
     * @property commitCount Commit count the build was made from.
     * @property versionName Version name of the build, such as `1.2.3`, compared for release builds.
     * @property repository GitHub `owner/repo` whose latest release is fetched.
     * @property syDebugVersion Build number of a preview build, such as `508`.
     * @property forceCheck Whether to ask GitHub even when the last check was less than 3 days ago.
     */
    public data class Arguments(
        val isPreview: Boolean,
        val commitCount: Int,
        val versionName: String,
        val repository: String,
        // SY -->
        val syDebugVersion: String,
        // SY <--
        val forceCheck: Boolean = false,
    )

    /** The outcome of an update check. */
    public sealed interface Result {
        /**
         * A newer release exists.
         *
         * @property release The release to offer.
         */
        public data class NewUpdate(val release: Release) : Result

        /** The running build is current, or the check was skipped as too recent. */
        public data object NoNewUpdate : Result

        /** This Android version is no longer supported, so no update is offered. */
        public data object OsTooOld : Result
    }

    private companion object {
        // Limit checks to once every 3 days at most
        const val CHECK_INTERVAL_DAYS = 3L
    }
}
