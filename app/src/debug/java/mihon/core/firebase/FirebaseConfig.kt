package mihon.core.firebase

import android.content.Context

/** No Firebase in this build: every hook is a no-op. */
internal object FirebaseConfig {
    fun init(context: Context) {
        // Nothing to initialise without Firebase.
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        // No analytics to toggle.
    }

    fun setCrashlyticsEnabled(enabled: Boolean) {
        // No crash reporting to toggle.
    }
}
