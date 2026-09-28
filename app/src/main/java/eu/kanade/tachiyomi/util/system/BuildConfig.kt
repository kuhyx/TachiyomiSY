package eu.kanade.tachiyomi.util.system

import eu.kanade.tachiyomi.BuildConfig
import exh.SY_DEBUG_VERSION

// SY -->
// A getter, not BuildConfig.DEBUG inline: tests of the debug variant stub it to reach release-only paths.
internal val isDebuggable: Boolean
    get() = BuildConfig.DEBUG
// SY <--

internal val isDebugBuildType: Boolean
    get() = BuildConfig.BUILD_TYPE == "debug"

internal val isPreviewBuildType: Boolean
    get() = BuildConfig.BUILD_TYPE == "release" /* SY --> */ && SY_DEBUG_VERSION != "0" /* SY <-- */

internal val isReleaseBuildType: Boolean
    get() = BuildConfig.BUILD_TYPE == "release" /* SY --> */ && SY_DEBUG_VERSION == "0" /* SY <-- */

// SY --> A plain getter (not inline) so tests can stub it; `or` evaluates both, so no branch the debug build misses.
internal val isBenchmarkBuildType: Boolean
    get() = BuildConfig.BUILD_TYPE.contains("nonMinified") or BuildConfig.BUILD_TYPE.contains("benchmark")

internal val isReleaseTestBuildType: Boolean
    get() = BuildConfig.BUILD_TYPE == "releaseTest"
// SY <--
