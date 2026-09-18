package eu.kanade.tachiyomi.util.storage

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
 * A [PreferenceStore] whose preferences really emit on [Preference.changes];
 * the in-module in-memory store completes its change flow without emitting.
 */
internal class FlowPreferenceStore : PreferenceStore {
    override fun getString(key: String, defaultValue: String): Preference<String> = FlowPreference(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = FlowPreference(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = FlowPreference(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = FlowPreference(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        FlowPreference(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        FlowPreference(key, defaultValue)

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = FlowPreference(key, defaultValue)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = FlowPreference(key, defaultValue)

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = FlowPreference(key, defaultValue)

    override fun getAll(): Map<String, *> = emptyMap<String, Any?>()
}

/** A preference backed by a [MutableStateFlow], so [changes] emits the current value and every update. */
internal class FlowPreference<T>(
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {
    private val state = MutableStateFlow<T?>(null)

    override fun key(): String = key

    override fun get(): T = state.value ?: defaultValue

    override fun set(value: T) {
        state.value = value
    }

    override fun isSet(): Boolean = state.value != null

    override fun delete() {
        state.value = null
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> = state.map { it ?: defaultValue }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = changes().stateIn(scope, SharingStarted.Eagerly, get())
}
