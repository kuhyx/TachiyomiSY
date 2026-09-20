package eu.kanade.presentation.components

/**
 * Reconciles a caller-owned query `String` with a text field that owns its own state.
 *
 * The caller's copy trails the field by at least a frame (a screen model's `StateFlow` collected
 * back into composition), so a value the field reported a moment ago comes back while the field
 * has already moved on. Writing that stale echo into the field would erase the keystrokes typed
 * in between -- burst-typing "Chainsmoker" landed as "Cinmo" -- so every reported value is kept
 * until the caller echoes it, and only a value the field never reported (a clear, a restore)
 * overwrites the field.
 */
internal class SearchQueryBridge(initial: String) {
    private val pending = ArrayDeque<String>()
    private var last = initial

    /** The field's text changed; true when the caller should be told about [text]. */
    fun fieldChanged(text: String): Boolean {
        if (text == last) return false
        last = text
        pending.addLast(text)
        return true
    }

    /** The caller's query changed; true when the field must be overwritten with [query]. */
    fun callerChanged(query: String): Boolean {
        val echoed = pending.indexOf(query)
        if (echoed >= 0) {
            repeat(echoed + 1) { pending.removeFirst() }
            return false
        }
        val overwrite = query != last
        if (overwrite) {
            pending.clear()
            last = query
        }
        return overwrite
    }
}
