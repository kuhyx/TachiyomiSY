package eu.kanade.tachiyomi.ui.base

import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * A [MapPreferenceStore] whose string-set preferences report their changes through [source] instead: a screen
 * model loading from one can be held in its loading state ([silentStore]) or see its source complete ([oneShotStore]).
 */
internal class StringSetChangesStore(
    private val source: (Preference<Set<String>>) -> Flow<Set<String>>,
    private val inner: PreferenceStore = MapPreferenceStore(),
) : PreferenceStore by inner {
    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        val preference = inner.getStringSet(key, defaultValue)
        return object : Preference<Set<String>> by preference {
            override fun changes(): Flow<Set<String>> = source(preference)
        }
    }
}

/** String sets never emit a change. */
internal fun silentStore(): PreferenceStore = StringSetChangesStore(source = { flow { awaitCancellation() } })

/** String sets emit their current value once and complete. */
internal fun oneShotStore(): PreferenceStore = StringSetChangesStore(source = { flowOf(it.get()) })
