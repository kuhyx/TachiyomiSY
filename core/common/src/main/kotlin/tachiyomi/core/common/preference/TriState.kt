package tachiyomi.core.common.preference

/** A filter that is off, matching, or excluding. */
public enum class TriState {
    /** Filter off. */
    DISABLED, // Disable filter

    /** Keep matching items. */
    ENABLED_IS, // Enabled with "is" filter

    /** Drop matching items. */
    ENABLED_NOT, // Enabled with "not" filter
    ;

    /** The state after one tap. */
    public fun next(): TriState = when (this) {
        DISABLED -> ENABLED_IS
        ENABLED_IS -> ENABLED_NOT
        ENABLED_NOT -> DISABLED
    }
}
