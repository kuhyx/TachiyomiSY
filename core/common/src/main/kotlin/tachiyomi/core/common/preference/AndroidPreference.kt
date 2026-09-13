package tachiyomi.core.common.preference

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.util.system.logcat

/** A [Preference] stored in [SharedPreferences], one subclass per storage type. */
public sealed class AndroidPreference<T>(
    private val preferences: SharedPreferences,
    private val keyFlow: Flow<String?>,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    /** Reads the stored value or [defaultValue]. */
    public abstract fun read(preferences: SharedPreferences, key: String, defaultValue: T): T

    /** The editor action that stores [value]. */
    public abstract fun write(key: String, value: T): Editor.() -> Unit

    override fun key(): String = key

    override fun get(): T = try {
        read(preferences, key, defaultValue)
    } catch (_: ClassCastException) {
        logcat { "Invalid value for $key; deleting" }
        delete()
        defaultValue
    }

    override fun set(value: T) {
        preferences.edit(action = write(key, value))
    }

    override fun isSet(): Boolean = preferences.contains(key)

    override fun delete() {
        preferences.edit {
            remove(key)
        }
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> = keyFlow
        .filter { it == key || it == null }
        .onStart { emit("ignition") }
        .map { get() }
        .conflate()

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = changes().stateIn(scope, SharingStarted.Eagerly, get())

    /** A string preference. */
    public class StringPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: String,
    ) : AndroidPreference<String>(preferences, keyFlow, key, defaultValue) {
        override fun read(
            preferences: SharedPreferences,
            key: String,
            defaultValue: String,
        ): String = preferences.getString(key, defaultValue) ?: defaultValue

        override fun write(key: String, value: String): Editor.() -> Unit = {
            putString(key, value)
        }
    }

    /** A long preference. */
    public class LongPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Long,
    ) : AndroidPreference<Long>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Long): Long =
            preferences.getLong(key, defaultValue)

        override fun write(key: String, value: Long): Editor.() -> Unit = {
            putLong(key, value)
        }
    }

    /** An int preference. */
    public class IntPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Int,
    ) : AndroidPreference<Int>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Int): Int =
            preferences.getInt(key, defaultValue)

        override fun write(key: String, value: Int): Editor.() -> Unit = {
            putInt(key, value)
        }
    }

    /** A float preference. */
    public class FloatPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Float,
    ) : AndroidPreference<Float>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Float): Float =
            preferences.getFloat(key, defaultValue)

        override fun write(key: String, value: Float): Editor.() -> Unit = {
            putFloat(key, value)
        }
    }

    /** A boolean preference. */
    public class BooleanPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Boolean,
    ) : AndroidPreference<Boolean>(preferences, keyFlow, key, defaultValue) {
        override fun read(
            preferences: SharedPreferences,
            key: String,
            defaultValue: Boolean,
        ): Boolean = preferences.getBoolean(key, defaultValue)

        override fun write(key: String, value: Boolean): Editor.() -> Unit = {
            putBoolean(key, value)
        }
    }

    /** A string-set preference. */
    public class StringSetPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Set<String>,
    ) : AndroidPreference<Set<String>>(preferences, keyFlow, key, defaultValue) {
        override fun read(
            preferences: SharedPreferences,
            key: String,
            defaultValue: Set<String>,
        ): Set<String> = preferences.getStringSet(key, defaultValue) ?: defaultValue

        override fun write(key: String, value: Set<String>): Editor.() -> Unit = {
            putStringSet(key, value)
        }
    }

    /** An object stored through a string conversion. */
    public class ObjectAsString<T>(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T,
    ) : AndroidPreference<T>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: T): T = try {
            preferences.getString(key, null)?.let(deserializer) ?: defaultValue
        } catch (_: Exception) {
            defaultValue
        }

        override fun write(key: String, value: T): Editor.() -> Unit = {
            putString(key, serializer(value))
        }
    }

    /** An object stored through an int conversion. */
    public class ObjectAsInt<T>(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> Int,
        private val deserializer: (Int) -> T,
    ) : AndroidPreference<T>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: T): T = try {
            if (preferences.contains(key)) preferences.getInt(key, 0).let(deserializer) else defaultValue
        } catch (_: Exception) {
            defaultValue
        }

        override fun write(key: String, value: T): Editor.() -> Unit = {
            putInt(key, serializer(value))
        }
    }

    /** A set of objects stored through string conversions. */
    public class ObjectSetAsStringSet<T>(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Set<T>,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T?,
    ) : AndroidPreference<Set<T>>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Set<T>): Set<T> = try {
            preferences.getStringSet(key, null)?.mapNotNull(deserializer)?.toSet() ?: defaultValue
        } catch (_: Exception) {
            defaultValue
        }

        override fun write(key: String, value: Set<T>): Editor.() -> Unit = {
            putStringSet(key, value.map(serializer).toSet())
        }
    }
}
