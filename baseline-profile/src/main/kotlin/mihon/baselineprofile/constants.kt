package mihon.baselineprofile

import androidx.test.platform.app.InstrumentationRegistry

/** How long a UI Automator lookup waits for the target app's screen before failing. */
internal const val UI_WAIT_TIMEOUT_MS: Long = 60_000L

/** The application id under test, passed by Gradle as the `targetAppId` instrumentation argument. */
internal val TARGET_PACKAGE_NAME: String
    inline get() = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: error("targetAppId not passed as instrumentation runner arg")
