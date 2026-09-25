package eu.kanade.tachiyomi.data.download

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
 * A [PreferenceStore] that keeps one value per key across lookups and whose [Preference.changes]
 * really emits: the in-module in-memory store hands out a fresh, detached preference on every
 * `getX(key)` call, so a value set through `setCredentials` would never be read back.
 */
internal class MapPreferenceStore : PreferenceStore {
    private val states = mutableMapOf<String, MutableStateFlow<Any?>>()

    private inline fun <reified T : Any> preference(key: String, defaultValue: T): Preference<T> =
        MapPreference(key, defaultValue, states.getOrPut(key) { MutableStateFlow(null) }, T::class.java)

    override fun getString(key: String, defaultValue: String): Preference<String> = preference(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = preference(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = preference(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = preference(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = preference(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        preference(key, defaultValue)

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = MappedPreference(getString(key, serializer(defaultValue)), serializer, deserializer)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = MappedPreference(getInt(key, serializer(defaultValue)), serializer, deserializer)

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = MappedPreference(
        inner = getStringSet(key, defaultValue.map(serializer).toSet()),
        serializer = { set -> set.map(serializer).toSet() },
        deserializer = { set -> set.mapNotNull(deserializer).toSet() },
    )

    override fun getAll(): Map<String, *> = states.mapValues { it.value.value }
}

/** A preference whose value lives in a [MutableStateFlow] shared by every lookup of its key. */
internal class MapPreference<T : Any>(
    private val key: String,
    private val defaultValue: T,
    private val state: MutableStateFlow<Any?>,
    private val type: Class<T>,
) : Preference<T> {
    override fun key(): String = key

    override fun get(): T = type.cast(state.value) ?: defaultValue

    override fun set(value: T) {
        state.value = value
    }

    override fun isSet(): Boolean = state.value != null

    override fun delete() {
        state.value = null
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> = state.map { type.cast(it) ?: defaultValue }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = changes().stateIn(scope, SharingStarted.Eagerly, get())
}

/** A preference stored as [S] through [serializer]/[deserializer], the way the Android store does it. */
internal class MappedPreference<T, S>(
    private val inner: Preference<S>,
    private val serializer: (T) -> S,
    private val deserializer: (S) -> T,
) : Preference<T> {
    override fun key(): String = inner.key()

    override fun get(): T = deserializer(inner.get())

    override fun set(value: T) = inner.set(serializer(value))

    override fun isSet(): Boolean = inner.isSet()

    override fun delete() = inner.delete()

    override fun defaultValue(): T = deserializer(inner.defaultValue())

    override fun changes(): Flow<T> = inner.changes().map(deserializer)

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = changes().stateIn(scope, SharingStarted.Eagerly, get())
}
