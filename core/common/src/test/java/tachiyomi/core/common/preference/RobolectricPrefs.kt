package tachiyomi.core.common.preference

import android.content.Context
import android.content.SharedPreferences
import org.robolectric.RuntimeEnvironment

/** A named, initially empty [SharedPreferences] of the Robolectric application. */
internal fun robolectricPrefs(name: String = "test-prefs"): SharedPreferences =
    RuntimeEnvironment.getApplication().getSharedPreferences(name, Context.MODE_PRIVATE)
