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

internal val isBenchmarkBuildType: Boolean
    inline get() = BuildConfig.BUILD_TYPE.contains("nonMinified") || BuildConfig.BUILD_TYPE.contains("benchmark")
