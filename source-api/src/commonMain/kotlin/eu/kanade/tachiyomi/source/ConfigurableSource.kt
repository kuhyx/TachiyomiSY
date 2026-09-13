package eu.kanade.tachiyomi.source

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** A source with its own settings screen. */

public interface ConfigurableSource : Source {

    /**
     * Gets instance of [SharedPreferences] scoped to the specific source.
     *
     * @since extensions-lib 1.5
     */
    public fun getSourcePreferences(): SharedPreferences =
        Injekt.get<Application>().getSharedPreferences(preferenceKey(), Context.MODE_PRIVATE)

    /** Adds the source's preferences to [screen]. */

    public fun setupPreferenceScreen(screen: PreferenceScreen)
}

/** Name of the preference file of this source. */

public fun ConfigurableSource.preferenceKey(): String = "source_$id"

// Pending upstream: switch to getSourcePreferences once all extensions are on ext-lib 1.5.

/** The preference file of this source. */
public fun ConfigurableSource.sourcePreferences(): SharedPreferences =
    Injekt.get<Application>().getSharedPreferences(preferenceKey(), Context.MODE_PRIVATE)

/** The preference file named [key]. */

public fun sourcePreferences(key: String): SharedPreferences =
    Injekt.get<Application>().getSharedPreferences(key, Context.MODE_PRIVATE)
