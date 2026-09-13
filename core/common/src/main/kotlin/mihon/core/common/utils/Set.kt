package mihon.core.common.utils

/** A copy of this set changed by [action]. */
public fun <T> Set<T>.mutate(action: (MutableSet<T>) -> Unit): Set<T> = toMutableSet().apply(action)
