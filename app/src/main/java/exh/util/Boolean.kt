package exh.util

internal infix fun <T : Comparable<T>> T.over(other: T) = this > other

internal infix fun <T : Comparable<T>> T.overEq(other: T) = this >= other

internal infix fun <T : Comparable<T>> T.under(other: T) = this < other

internal infix fun <T : Comparable<T>> T.underEq(other: T) = this <= other
