package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * One stored setting with change notifications.
 *
 * @param T the value type.
 */
public interface Preference<T> {

    /** The storage key. */
    public fun key(): String

    /** The current value or the default. */
    public fun get(): T

    /** Stores [value]. */
    public fun set(value: T)

    /** True when a value is stored. */
    public fun isSet(): Boolean

    /** Removes the stored value. */
    public fun delete()

    /** The value used when nothing is stored. */
    public fun defaultValue(): T

    /** Emits every stored value, starting with the current one. */
    public fun changes(): Flow<T>

    /** [changes] as a state flow in [scope]. */
    public fun stateIn(scope: CoroutineScope): StateFlow<T>

    /** Key prefixes that mark preferences as private or as app state. */
    public companion object {
        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"

        /**
         * A preference that should not be exposed in places like backups without user consent.
         */
        public fun isPrivate(key: String): Boolean = key.startsWith(PRIVATE_PREFIX)

        /** [key] marked as private. */
        public fun privateKey(key: String): String = "$PRIVATE_PREFIX$key"

        /**
         * A preference used for internal app state that isn't really a user preference
         * and therefore should not be in places like backups.
         */
        public fun isAppState(key: String): Boolean = key.startsWith(APP_STATE_PREFIX)

        /** [key] marked as app state. */
        public fun appStateKey(key: String): String = "$APP_STATE_PREFIX$key"
    }
}

/** Stores the result of [block] applied to the current value. */
public inline fun <reified T, R : T> Preference<T>.getAndSet(crossinline block: (T) -> R) {
    set(block(get()))
}

/** Adds [item] to the stored set. */
public operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

/** Adds [items] to the stored set. */
public operator fun <T> Preference<Set<T>>.plusAssign(items: Iterable<T>) {
    set(get() + items)
}

/** Removes [item] from the stored set. */
public operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

/** Flips the value and returns the new one. */
public fun Preference<Boolean>.toggle(): Boolean {
    set(!get())
    return get()
}
