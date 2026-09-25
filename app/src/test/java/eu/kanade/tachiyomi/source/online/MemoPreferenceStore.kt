package eu.kanade.tachiyomi.source.online

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
 * A [PreferenceStore] that hands out one live preference per key, so a value set through one
 * lookup is read by the next, and whose [Preference.changes] emit (the in-memory store's do not).
 */
internal class MemoPreferenceStore : PreferenceStore {
    private val strings = mutableMapOf<String, MemoPreference<String>>()
    private val longs = mutableMapOf<String, MemoPreference<Long>>()
    private val ints = mutableMapOf<String, MemoPreference<Int>>()
    private val floats = mutableMapOf<String, MemoPreference<Float>>()
    private val booleans = mutableMapOf<String, MemoPreference<Boolean>>()
    private val stringSets = mutableMapOf<String, MemoPreference<Set<String>>>()

    override fun getString(key: String, defaultValue: String): Preference<String> =
        strings.getOrPut(key) { MemoPreference(key, defaultValue) }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
        longs.getOrPut(key) { MemoPreference(key, defaultValue) }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> =
        ints.getOrPut(key) { MemoPreference(key, defaultValue) }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
        floats.getOrPut(key) { MemoPreference(key, defaultValue) }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        booleans.getOrPut(key) { MemoPreference(key, defaultValue) }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        stringSets.getOrPut(key) { MemoPreference(key, defaultValue) }

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = MappedPreference(getString(key), defaultValue, serializer, deserializer)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = MappedPreference(getInt(key), defaultValue, serializer, deserializer)

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = MappedPreference(
        backing = getStringSet(key),
        defaultValue = defaultValue,
        serializer = { set -> set.map(serializer).toSet() },
        deserializer = { set -> set.mapNotNull(deserializer).toSet() },
    )

    override fun getAll(): Map<String, *> = strings + longs + ints + floats + booleans + stringSets

    /** Clears every value while keeping the preference objects, so earlier lookups stay live. */
    fun reset() {
        getAll().values.forEach { (it as MemoPreference<*>).delete() }
    }
}

/** One preference value with a live [changes] flow. */
internal class MemoPreference<T>(private val key: String, private val default: T) : Preference<T> {
    private val data = MutableStateFlow<T?>(null)

    override fun key(): String = key

    override fun get(): T = data.value ?: default

    override fun set(value: T) {
        data.value = value
    }

    override fun isSet(): Boolean = data.value != null

    override fun delete() {
        data.value = null
    }

    override fun defaultValue(): T = default

    override fun changes(): Flow<T> = data.map { it ?: default }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> =
        changes().stateIn(scope, SharingStarted.Eagerly, get())
}

/** A typed view over a [backing] preference of another type. */
internal class MappedPreference<T, B>(
    private val backing: Preference<B>,
    private val defaultValue: T,
    private val serializer: (T) -> B,
    private val deserializer: (B) -> T,
) : Preference<T> {
    override fun key(): String = backing.key()

    override fun get(): T = if (backing.isSet()) deserializer(backing.get()) else defaultValue

    override fun set(value: T) = backing.set(serializer(value))

    override fun isSet(): Boolean = backing.isSet()

    override fun delete() = backing.delete()

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> = backing.changes().map { get() }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> =
        changes().stateIn(scope, SharingStarted.Eagerly, get())
}
