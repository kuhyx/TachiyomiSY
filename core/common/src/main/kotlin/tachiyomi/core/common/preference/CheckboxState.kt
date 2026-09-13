package tachiyomi.core.common.preference

/**
 * A value paired with its checkbox selection.
 * @param T the value type.
 * @property value the wrapped value.
 */
public sealed class CheckboxState<T>(public open val value: T) {

    /** The state after one tap. */
    public abstract fun next(): CheckboxState<T>

    /** Two-way checkbox: checked or not. */
    public sealed class State<T>(override val value: T) : CheckboxState<T>(value) {
        /** Checked. */
        public data class Checked<T>(override val value: T) : State<T>(value)

        /** Not checked. */
        public data class None<T>(override val value: T) : State<T>(value)

        /** True for [Checked]. */
        public val isChecked: Boolean
            get() = this is Checked

        override fun next(): CheckboxState<T> = when (this) {
            is Checked -> None(value)
            is None -> Checked(value)
        }
    }

    /** Three-way checkbox: include, exclude or neither. */
    public sealed class TriState<T>(override val value: T) : CheckboxState<T>(value) {
        /** Included. */
        public data class Include<T>(override val value: T) : TriState<T>(value)

        /** Excluded. */
        public data class Exclude<T>(override val value: T) : TriState<T>(value)

        /** Neither. */
        public data class None<T>(override val value: T) : TriState<T>(value)

        override fun next(): CheckboxState<T> = when (this) {
            is Exclude -> None(value)
            is Include -> Exclude(value)
            is None -> Include(value)
        }
    }
}

/** [CheckboxState.State.Checked] when [condition] holds, else [CheckboxState.State.None]. */
public inline fun <T> T.asCheckboxState(condition: (T) -> Boolean): CheckboxState.State<T> = if (condition(this)) {
    CheckboxState.State.Checked(this)
} else {
    CheckboxState.State.None(this)
}

/** Each element as a checkbox state per [condition]. */
public inline fun <T> List<T>.mapAsCheckboxState(condition: (T) -> Boolean): List<CheckboxState.State<T>> =
    this.map { it.asCheckboxState(condition) }
