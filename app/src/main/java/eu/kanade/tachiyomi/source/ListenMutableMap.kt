package eu.kanade.tachiyomi.source

import uy.kohesive.injekt.api.get

internal class ListenMutableMap<K, V>(
    private val internalMap: MutableMap<K, V>,
    private val listener: () -> Unit,
) : MutableMap<K, V> by internalMap {
    override fun clear() {
        val clearResult = internalMap.clear()
        listener()
        return clearResult
    }

    override fun put(key: K, value: V): V? {
        val putResult = internalMap.put(key, value)
        if (putResult == null) {
            listener()
        }
        return putResult
    }

    override fun putAll(from: Map<out K, V>) {
        internalMap.putAll(from)
        listener()
    }

    override fun remove(key: K): V? {
        val removeResult = internalMap.remove(key)
        if (removeResult != null) {
            listener()
        }
        return removeResult
    }
}
