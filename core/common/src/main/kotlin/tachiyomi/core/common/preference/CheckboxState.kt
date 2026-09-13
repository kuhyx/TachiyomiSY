package tachiyomi.core.common.preference

public sealed class CheckboxState<T>(public open val value: T) {

    public abstract fun next(): CheckboxState<T>

    public sealed class State<T>(override val value: T) : CheckboxState<T>(value) {
        public data class Checked<T>(override val value: T) : State<T>(value)
        public data class None<T>(override val value: T) : State<T>(value)

        public val isChecked: Boolean
            get() = this is Checked

        override fun next(): CheckboxState<T> {
            return when (this) {
                is Checked -> None(value)
                is None -> Checked(value)
            }
        }
    }

    public sealed class TriState<T>(override val value: T) : CheckboxState<T>(value) {
        public data class Include<T>(override val value: T) : TriState<T>(value)
        public data class Exclude<T>(override val value: T) : TriState<T>(value)
        public data class None<T>(override val value: T) : TriState<T>(value)

        override fun next(): CheckboxState<T> {
            return when (this) {
                is Exclude -> None(value)
                is Include -> Exclude(value)
                is None -> Include(value)
            }
        }
    }
}

public inline fun <T> T.asCheckboxState(condition: (T) -> Boolean): CheckboxState.State<T> {
    return if (condition(this)) {
        CheckboxState.State.Checked(this)
    } else {
        CheckboxState.State.None(this)
    }
}

public inline fun <T> List<T>.mapAsCheckboxState(condition: (T) -> Boolean): List<CheckboxState.State<T>> {
    return this.map { it.asCheckboxState(condition) }
}
