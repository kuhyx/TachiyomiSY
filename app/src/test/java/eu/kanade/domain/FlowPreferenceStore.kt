package eu.kanade.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * An in-memory [PreferenceStore] whose preferences persist across lookups of the same key and whose
 * [Preference.changes] emits the current value first, like the Android-backed store does. Unlike
 * [tachiyomi.core.common.preference.InMemoryPreferenceStore], a value written through one lookup is seen
 * by the next, which is what code that re-reads a preference (or observes it) needs. Objects are kept
 * in their serialized form, so the serializer and deserializer run exactly as they do in production.
 */
internal class FlowPreferenceStore : PreferenceStore {

    private val strings = mutableMapOf<String, MutableStateFlow<String?>>()
    private val longs = mutableMapOf<String, MutableStateFlow<Long?>>()
    private val ints = mutableMapOf<String, MutableStateFlow<Int?>>()
    private val floats = mutableMapOf<String, MutableStateFlow<Float?>>()
    private val booleans = mutableMapOf<String, MutableStateFlow<Boolean?>>()
    private val stringSets = mutableMapOf<String, MutableStateFlow<Set<String>?>>()

    private fun <S> MutableMap<String, MutableStateFlow<S?>>.state(key: String): MutableStateFlow<S?> =
        getOrPut(key) { MutableStateFlow(null) }

    override fun getString(key: String, defaultValue: String): Preference<String> =
        FlowPreference(key, strings.state(key), defaultValue, { it }, { it })

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
        FlowPreference(key, longs.state(key), defaultValue, { it }, { it })

    override fun getInt(key: String, defaultValue: Int): Preference<Int> =
        FlowPreference(key, ints.state(key), defaultValue, { it }, { it })

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
        FlowPreference(key, floats.state(key), defaultValue, { it }, { it })

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        FlowPreference(key, booleans.state(key), defaultValue, { it }, { it })

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        FlowPreference(key, stringSets.state(key), defaultValue, { it }, { it })

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = FlowPreference(key, strings.state(key), defaultValue, serializer, deserializer)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = FlowPreference(key, ints.state(key), defaultValue, serializer, deserializer)

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = FlowPreference(
        key = key,
        state = stringSets.state(key),
        defaultValue = defaultValue,
        encode = { values -> values.map(serializer).toSet() },
        decode = { values -> values.mapNotNull(deserializer).toSet() },
    )

    override fun getAll(): Map<String, *> =
        listOf(strings, longs, ints, floats, booleans, stringSets)
            .flatMap { map -> map.entries.mapNotNull { (key, state) -> state.value?.let { key to it } } }
            .toMap()

    private class FlowPreference<S, T>(
        private val key: String,
        private val state: MutableStateFlow<S?>,
        private val defaultValue: T,
        private val encode: (T) -> S,
        private val decode: (S) -> T,
    ) : Preference<T> {
        override fun key(): String = key

        override fun get(): T = state.value?.let(decode) ?: defaultValue

        override fun set(value: T) {
            state.value = encode(value)
        }

        override fun isSet(): Boolean = state.value != null

        override fun delete() {
            state.value = null
        }

        override fun defaultValue(): T = defaultValue

        override fun changes(): Flow<T> = state.map { get() }

        override fun stateIn(scope: CoroutineScope): StateFlow<T> =
            changes().stateIn(scope, SharingStarted.Eagerly, get())
    }
}
