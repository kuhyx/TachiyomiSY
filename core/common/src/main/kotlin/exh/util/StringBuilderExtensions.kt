package exh.util

/** Appends [other]. */
public operator fun StringBuilder.plusAssign(other: String) {
    append(other)
}
