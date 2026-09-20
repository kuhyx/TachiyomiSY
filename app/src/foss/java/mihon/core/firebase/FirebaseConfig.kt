package mihon.core.firebase

import android.content.Context

/** No Firebase in this build: every hook is a no-op that keeps the release signatures. */
internal object FirebaseConfig {
    fun init(ignored: Context) {
        // Nothing to initialise without Firebase.
    }

    fun setAnalyticsEnabled(ignored: Boolean) {
        // No analytics to toggle.
    }

    fun setCrashlyticsEnabled(ignored: Boolean) {
        // No crash reporting to toggle.
    }
}
