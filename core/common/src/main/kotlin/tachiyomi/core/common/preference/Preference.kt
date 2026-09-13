package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public interface Preference<T> {

    public fun key(): String

    public fun get(): T

    public fun set(value: T)

    public fun isSet(): Boolean

    public fun delete()

    public fun defaultValue(): T

    public fun changes(): Flow<T>

    public fun stateIn(scope: CoroutineScope): StateFlow<T>

    public companion object {
        /**
         * A preference that should not be exposed in places like backups without user consent.
         */
        public fun isPrivate(key: String): Boolean {
            return key.startsWith(PRIVATE_PREFIX)
        }
        public fun privateKey(key: String): String {
            return "$PRIVATE_PREFIX$key"
        }

        /**
         * A preference used for internal app state that isn't really a user preference
         * and therefore should not be in places like backups.
         */
        public fun isAppState(key: String): Boolean {
            return key.startsWith(APP_STATE_PREFIX)
        }
        public fun appStateKey(key: String): String {
            return "$APP_STATE_PREFIX$key"
        }

        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"
    }
}

public inline fun <reified T, R : T> Preference<T>.getAndSet(crossinline block: (T) -> R) {
    set(block(get()))
}

public operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

public operator fun <T> Preference<Set<T>>.plusAssign(items: Iterable<T>) {
    set(get() + items)
}

public operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

public fun Preference<Boolean>.toggle(): Boolean {
    set(!get())
    return get()
}
