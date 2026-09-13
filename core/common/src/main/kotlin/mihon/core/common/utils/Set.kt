package mihon.core.common.utils

public fun <T> Set<T>.mutate(action: (MutableSet<T>) -> Unit): Set<T> {
    return toMutableSet().apply(action)
}
