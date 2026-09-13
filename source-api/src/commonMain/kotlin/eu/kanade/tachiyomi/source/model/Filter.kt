package eu.kanade.tachiyomi.source.model

public sealed class Filter<T>(public val name: String, public var state: T) {
    public open class Header(name: String) : Filter<Any>(name, 0)
    public open class Separator(name: String = "") : Filter<Any>(name, 0)
    public abstract class Select<V>(name: String, public val values: Array<V>, state: Int = 0) : Filter<Int>(
        name,
        state,
    )
    public abstract class Text(name: String, state: String = "") : Filter<String>(name, state)
    public abstract class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)
    public abstract class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
        public fun isIgnored(): Boolean = state == STATE_IGNORE
        public fun isIncluded(): Boolean = state == STATE_INCLUDE
        public fun isExcluded(): Boolean = state == STATE_EXCLUDE

        public companion object {
            public const val STATE_IGNORE: Int = 0
            public const val STATE_INCLUDE: Int = 1
            public const val STATE_EXCLUDE: Int = 2
        }
    }

    public abstract class Group<V>(name: String, state: List<V>) : Filter<List<V>>(name, state)

    public abstract class Sort(name: String, public val values: Array<String>, state: Selection? = null) :
        Filter<Sort.Selection?>(name, state) {
        public data class Selection(val index: Int, val ascending: Boolean)
    }

    // SY -->
    public abstract class AutoComplete(
        name: String,
        public val hint: String,
        public val values: List<String>,
        public val skipAutoFillTags: List<String> = emptyList(),
        public val validPrefixes: List<String> = emptyList(),
        state: List<String>,
    ) : Filter<List<String>>(name, state)
    // SY <--

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Filter<*>) return false

        return name == other.name && state == other.state
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        return result
    }
}
