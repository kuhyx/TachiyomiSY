package mihon.core.firebase

import android.content.Context

internal object FirebaseConfig {
    fun init(context: Context) = Unit

    fun setAnalyticsEnabled(enabled: Boolean) = Unit

    fun setCrashlyticsEnabled(enabled: Boolean) = Unit
}
