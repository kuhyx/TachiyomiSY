package eu.kanade.tachiyomi.data.track.anilist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * A [PreferenceStore] that hands out the same [Preference] for a key every time, so a `set` survives
 * the next `get` (the stock in-memory store rebuilds its preferences from the seed map on every call).
 * [clear] resets values in place, so a preference captured at construction time stays in sync.
 */
internal class MapPreferenceStore : PreferenceStore {
    private val strings = HashMap<String, FakePreference<String>>()
    private val longs = HashMap<String, FakePreference<Long>>()
    private val ints = HashMap<String, FakePreference<Int>>()
    private val floats = HashMap<String, FakePreference<Float>>()
    private val booleans = HashMap<String, FakePreference<Boolean>>()
    private val stringSets = HashMap<String, FakePreference<Set<String>>>()

    fun clear() {
        listOf(strings, longs, ints, floats, booleans, stringSets).forEach { map ->
            map.values.forEach(FakePreference<*>::reset)
        }
    }

    /** Makes every read of the string preference under [key] come from [reads] until the next [clear]. */
    fun scriptString(key: String, reads: () -> String) {
        strings.preference(key, "").reads = reads
    }

    /** Makes every read of the boolean preference under [key] come from [reads] until the next [clear]. */
    fun scriptBoolean(key: String, reads: () -> Boolean) {
        booleans.preference(key, false).reads = reads
    }

    override fun getString(key: String, defaultValue: String): Preference<String> =
        strings.preference(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = longs.preference(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = ints.preference(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = floats.preference(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        booleans.preference(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        stringSets.preference(key, defaultValue)

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = InMemoryPreference(key, null, defaultValue)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = InMemoryPreference(key, null, defaultValue)

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = InMemoryPreference(key, null, defaultValue)

    override fun getAll(): Map<String, *> = strings + longs + ints + floats + booleans + stringSets

    private fun <T> MutableMap<String, FakePreference<T>>.preference(key: String, defaultValue: T) =
        getOrPut(key) { FakePreference(key, defaultValue) }
}

/** An in-memory preference whose reads can be scripted (to fail, or to change between two reads). */
internal class FakePreference<T>(
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {
    private var data: T? = null
    var reads: (() -> T)? = null

    fun reset() {
        data = null
        reads = null
    }

    override fun key(): String = key

    override fun get(): T = reads?.invoke() ?: data ?: defaultValue

    override fun set(value: T) {
        data = value
    }

    override fun isSet(): Boolean = data != null

    override fun delete() {
        data = null
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> = MutableStateFlow(get())

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = MutableStateFlow(get()).asStateFlow()
}
