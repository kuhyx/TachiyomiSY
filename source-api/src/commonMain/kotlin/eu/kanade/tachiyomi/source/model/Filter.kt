package eu.kanade.tachiyomi.source.model

/**
 * One entry of a source's search filter list, holding a user-editable [state].
 *
 * @param T the type of [state].
 * @property name the label shown for the filter.
 * @property state the current value; sources read it when building the search request.
 */
public sealed class Filter<T>(public val name: String, public var state: T) {
    /** A non-interactive heading between filters. */
    public open class Header(name: String) : Filter<Any>(name, 0)

    /** A non-interactive divider between filters. */
    public open class Separator(name: String = "") : Filter<Any>(name, 0)

    /**
     * A single choice among [values]; the state is the selected index.
     *
     * @param V the option type.
     * @param name the label.
     * @property values the selectable options, rendered with `toString`.
     * @param state the initially selected index.
     */
    public abstract class Select<V>(name: String, public val values: Array<V>, state: Int = 0) : Filter<Int>(
        name,
        state,
    )

    /** A free-text field. */
    public abstract class Text(name: String, state: String = "") : Filter<String>(name, state)

    /** An on/off toggle. */
    public abstract class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)

    /** A three-way toggle: ignore, include or exclude. */
    public abstract class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
        /** True when the filter is neutral. */
        public fun isIgnored(): Boolean = state == STATE_IGNORE

        /** True when results must match. */
        public fun isIncluded(): Boolean = state == STATE_INCLUDE

        /** True when results must not match. */
        public fun isExcluded(): Boolean = state == STATE_EXCLUDE

        /** The three states. */
        public companion object {
            /** Neutral. */
            public const val STATE_IGNORE: Int = 0

            /** Must match. */
            public const val STATE_INCLUDE: Int = 1

            /** Must not match. */
            public const val STATE_EXCLUDE: Int = 2
        }
    }

    /** A collapsible group of nested filters. */
    public abstract class Group<V>(name: String, state: List<V>) : Filter<List<V>>(name, state)

    /**
     * A sort order chosen among [values], with direction.
     *
     * @param name the label.
     * @property values the sort criteria.
     * @param state the initial selection, or none.
     */
    public abstract class Sort(name: String, public val values: Array<String>, state: Selection? = null) :
        Filter<Sort.Selection?>(name, state) {
        /**
         * The chosen criterion and direction.
         *
         * @property index position in [values].
         * @property ascending true for ascending order.
         */
        public data class Selection(val index: Int, val ascending: Boolean)
    }

    // SY -->

    /**
     * SY: a tag field with completion, holding the entered tags as its state.
     *
     * @param name the label.
     * @property hint placeholder shown when empty.
     * @property values the completion candidates.
     * @property skipAutoFillTags candidates that are never auto-filled.
     * @property validPrefixes prefixes (such as `-`) accepted in front of a tag.
     * @param state the initially entered tags.
     */
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
